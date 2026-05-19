package com.kitradingbot.service;

import com.kitradingbot.feature.FeaturePipeline;
import com.kitradingbot.indicator.BbandsIndicator;
import com.kitradingbot.indicator.EmaIndicator;
import com.kitradingbot.indicator.RsiIndicator;
import com.kitradingbot.indicator.SmaIndicator;
import com.kitradingbot.model.Candle;
import com.kitradingbot.model.IndicatorConfig;
import com.kitradingbot.model.IndicatorValue;
import com.kitradingbot.model.MlPredictionResponse;
import com.kitradingbot.model.SignalDecision;
import com.kitradingbot.model.SignalSnapshot;
import com.kitradingbot.signal.SignalEngine;
import com.kitradingbot.trading.TradingEngine;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementierung des {@link SignalService}.
 * Diese Service-Klasse koordiniert den gesamten Datenfluss für die Signalanalyse:
 * von der Beschaffung der Marktdaten über die Feature-Berechnung und KI-Vorhersage
 * bis hin zur Generierung des Handelssignals.
 * Im Live-Betrieb triggert sie zudem die Ausführung von Trades durch die {@link TradingEngine}.
 */
@Service
public class SignalServiceImpl implements SignalService {
    private final MarketDataService marketDataService;
    private final FeaturePipeline featurePipeline;
    private final PredictionService predictionService;
    private final SignalEngine signalEngine;
    private final TradingEngine tradingEngine;

    // Indikatoren für die Berechnung der Werte im Snapshot
    private final SmaIndicator smaIndicator;
    private final RsiIndicator rsiIndicator;
    private final EmaIndicator emaIndicator;
    private final BbandsIndicator bbandsIndicator;


    /**
     * Konstruktor für den SignalServiceImpl.
     * Spring injiziert automatisch alle benötigten Abhängigkeiten.
     *
     * @param marketDataService Der Service zum Abrufen von Marktdaten.
     * @param featurePipeline   Die Pipeline zur Berechnung von Features für die KI.
     * @param predictionService Der Service zur Einholung von KI-Vorhersagen.
     * @param signalEngine      Die Engine zur Generierung von Handelssignalen.
     * @param tradingEngine     Die Engine zur Ausführung von Trades im Live-Betrieb.
     * @param smaIndicator      Der SMA-Indikator zur Berechnung von Werten für den Snapshot.
     * @param rsiIndicator      Der RSI-Indikator zur Berechnung von Werten für den Snapshot.
     * @param emaIndicator      Der EMA-Indikator zur Berechnung von Werten für den Snapshot.
     * @param bbandsIndicator   Der Bollinger Bands Indikator zur Berechnung von Werten für den Snapshot.
     */
    public SignalServiceImpl(
            MarketDataService marketDataService,
            FeaturePipeline featurePipeline,
            PredictionService predictionService,
            SignalEngine signalEngine,
            TradingEngine tradingEngine,
            SmaIndicator smaIndicator,
            RsiIndicator rsiIndicator,
            EmaIndicator emaIndicator,
            BbandsIndicator bbandsIndicator
    ) {
        this.marketDataService = marketDataService;
        this.featurePipeline = featurePipeline;
        this.predictionService = predictionService;
        this.signalEngine = signalEngine;
        this.tradingEngine = tradingEngine;
        this.smaIndicator = smaIndicator;
        this.rsiIndicator = rsiIndicator;
        this.emaIndicator = emaIndicator;
        this.bbandsIndicator = bbandsIndicator;
    }

    /**
     * Führt die vollständige Analyse-Pipeline für den aktuellen Zeitpunkt durch
     * und generiert einen {@link SignalSnapshot}.
     * Im Live-Betrieb wird hier auch versucht, einen Trade auszuführen, falls ein Signal vorliegt.
     *
     * @param symbol     Das Handelssymbol.
     * @param candles    Anzahl der Kerzen, die für die Analyse herangezogen werden.
     * @param useMl      Flag, ob die KI für den Signal-Konsens genutzt werden soll.
     * @param indicators Liste der dynamischen Indikator-Konfigurationen vom Frontend.
     * @return Ein {@link SignalSnapshot}-Objekt mit allen Analyseergebnissen.
     */
    @Override
    public SignalSnapshot currentSignal(String symbol, int candles, boolean useMl, List<IndicatorConfig> indicators) {
        // 1. Neueste Marktdaten laden
        List<Candle> market = marketDataService.getMarketData(symbol, candles);
        
        // 2. Technische Indikatoren (Features) berechnen, unter Berücksichtigung der dynamischen Konfiguration
        double[] features = featurePipeline.buildFeatures(market, indicators);
        
        // 3. Preisvorhersage vom ML-Modell einholen
        MlPredictionResponse prediction = predictionService.predict(features);
        
        // 4. Signal-Entscheidung basierend auf Strategieregeln generieren, unter Berücksichtigung der dynamischen Konfiguration
        SignalDecision decision = signalEngine.generateSignal(market, prediction, useMl, indicators);
        
        // Die aktuellste Kerze für die Trade-Ausführung und den Snapshot.
        Candle latest = market.get(market.size() - 1);
        
        // 5. Versuch der Trade-Ausführung (wird nur ausgeführt, wenn tradingEngine.isRunning() == true)
        tradingEngine.execute(decision, latest);

        // 6. Berechne die Werte der konfigurierten Indikatoren für den Snapshot
        List<IndicatorValue> indicatorValues = new ArrayList<>();
        int idx = market.size() - 1; // Index der aktuellsten Kerze
        for (IndicatorConfig config : indicators) {
            String type = config.type().toUpperCase();
            if ("SMA".equals(type)) {
                int len = config.getIntParam("length", 20);
                indicatorValues.add(new IndicatorValue("SMA " + len, smaIndicator.calculate(market, idx, len)));
            } else if ("EMA".equals(type)) {
                int len = config.getIntParam("length", 20);
                indicatorValues.add(new IndicatorValue("EMA " + len, emaIndicator.calculate(market, idx, len)));
            } else if ("RSI".equals(type)) {
                int len = config.getIntParam("length", 14);
                indicatorValues.add(new IndicatorValue("RSI " + len, rsiIndicator.calculate(market, idx, len)));
            } else if ("BBANDS".equals(type)) {
                int len = config.getIntParam("length", 20);
                double mult = config.getDoubleParam("mult", 2.0);
                BbandsIndicator.Bands bands = bbandsIndicator.calculate(market, idx, len, mult);
                indicatorValues.add(new IndicatorValue("BB Upper", bands.upper()));
                indicatorValues.add(new IndicatorValue("BB Middle", bands.middle()));
                indicatorValues.add(new IndicatorValue("BB Lower", bands.lower()));
            }
            // Hier könnten weitere Indikatoren hinzugefügt werden
        }

        // 7. Zusammenfassung für das Frontend zurückgeben
        return new SignalSnapshot(
                latest.close(),
                prediction,
                decision,
                tradingEngine.isRunning(),
                featurePipeline.featureNames(),
                features,
                indicatorValues // NEU: Liste der berechneten Indikatorwerte
        );
    }
}
