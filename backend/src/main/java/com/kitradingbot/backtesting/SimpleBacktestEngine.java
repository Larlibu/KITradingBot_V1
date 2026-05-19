package com.kitradingbot.backtesting;

import com.kitradingbot.feature.FeaturePipeline;
import com.kitradingbot.integration.MlServiceClient;
import com.kitradingbot.model.BacktestResult;
import com.kitradingbot.model.Candle;
import com.kitradingbot.model.IndicatorConfig;
import com.kitradingbot.model.MlPredictionResponse;
import com.kitradingbot.model.SignalDecision;
import com.kitradingbot.model.SignalType;
import com.kitradingbot.model.Trade;
import com.kitradingbot.service.MarketDataService;
import com.kitradingbot.signal.SignalEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Eine einfache Implementierung der {@link BacktestEngine}.
 * Diese Engine simuliert Handelsstrategien auf Basis historischer Marktdaten unter Verwendung
 * von Indikatoren, ML-Vorhersagen und Signalentscheidungen. Sie ist optimiert für Performance
 * durch Batch-Prediction des ML-Services.
 */
@Component
public class SimpleBacktestEngine implements BacktestEngine {
    private static final Logger log = LoggerFactory.getLogger(SimpleBacktestEngine.class);
    
    private final MarketDataService marketDataService;
    private final FeaturePipeline featurePipeline;
    private final MlServiceClient mlServiceClient;
    private final SignalEngine signalEngine;

    // Interne Zustandsvariablen für die Simulation eines Backtest-Laufs
    private double currentCash; // Aktuelles virtuelles Kapital während der Simulation
    private double currentQuantity; // Menge des aktuell gehaltenen Assets
    private double currentEntryPrice; // Einstiegspreis der aktuellen offenen Position
    private int totalWins; // Zähler für gewonnene Trades
    private int totalLosses; // Zähler für verlorene Trades
    private int totalTradesCount; // Gesamtzahl der abgeschlossenen Trades
    private List<Trade> trades; // Liste aller während des Backtests ausgeführten Trades
    private long tradeIdCounter; // Zähler für eindeutige Trade-IDs

    /**
     * Konstruktor für die SimpleBacktestEngine.
     * Spring injiziert automatisch die benötigten Abhängigkeiten.
     *
     * @param marketDataService Der Service zum Abrufen von Marktdaten.
     * @param featurePipeline   Die Pipeline zur Berechnung von Features für die KI.
     * @param mlServiceClient   Der Client für die Kommunikation mit dem ML-Service.
     * @param signalEngine      Die Engine zur Generierung von Handelssignalen.
     */
    public SimpleBacktestEngine(
            MarketDataService marketDataService,
            FeaturePipeline featurePipeline,
            MlServiceClient mlServiceClient,
            SignalEngine signalEngine
    ) {
        this.marketDataService = marketDataService;
        this.featurePipeline = featurePipeline;
        this.mlServiceClient = mlServiceClient;
        this.signalEngine = signalEngine;
    }

    /**
     * Führt einen Backtest für eine Handelsstrategie auf historischen Daten durch.
     * Der Prozess umfasst:
     * 1. Abrufen der Marktdaten.
     * 2. Vorbereiten der Features für alle Kerzen.
     * 3. Anfordern von Batch-Vorhersagen vom ML-Service.
     * 4. Iteration durch die Kerzen, Generierung von Signalen und Simulation von Trades.
     * 5. Berechnung der Performance-Metriken.
     *
     * @param symbol     Das Handelssymbol, für das der Backtest durchgeführt werden soll.
     * @param candles    Die Anzahl der historischen Kerzen, die für die Simulation verwendet werden sollen.
     * @param useMl      Ein Flag, das angibt, ob die KI-Vorhersage in die Signalgenerierung einbezogen werden soll.
     * @param indicators Eine Liste von {@link IndicatorConfig}-Objekten, die die Konfiguration der
     *                   vom Benutzer ausgewählten Indikatoren definieren.
     * @return Ein {@link BacktestResult}-Objekt, das die Performance-Metriken,
     *         die historischen Kerzen und die simulierten Trades des Backtests enthält.
     */
    @Override
    public BacktestResult run(String symbol, int candles, boolean useMl, List<IndicatorConfig> indicators) {
        log.info("Starte Backtest für {} ({} Kerzen, useMl={}, Indikatoren: {})", symbol, candles, useMl, indicators.size());
        List<Candle> market = marketDataService.getMarketData(symbol, candles);
        int warmup = featurePipeline.warmupPeriod();
        
        // 1. Batch-Vorbereitung: Berechne Features und hole Vorhersagen nur, wenn useMl aktiv ist.
        List<MlPredictionResponse> predictions = new ArrayList<>();
        if (useMl) {
            List<double[]> allFeatures = new ArrayList<>();
            for (int i = warmup; i < market.size(); i++) {
                allFeatures.add(featurePipeline.buildFeatures(market.subList(0, i + 1), indicators));
            }
            log.info("Sende Batch-Prediction für {} Ticks an den ML-Service.", allFeatures.size());
            try {
                predictions = mlServiceClient.predictBatch(allFeatures);
            } catch (Exception e) {
                log.error("Fehler bei der Batch-Prediction: {}. Backtest wird ohne KI fortgesetzt.", e.getMessage());
                useMl = false; // Fallback: KI deaktivieren, wenn der Service einen Fehler wirft
            }
        }

        // 2. Simulation: Initialisierung des Zustands und Iteration durch die Kerzen.
        double initialCapital = 10_000.0;
        initSimulation(initialCapital);

        // Bestimme die Start-Iteration (entweder nach Warmup oder ab Index 0)
        int startIdx = useMl ? warmup : 1; 

        for (int i = startIdx; i < market.size(); i++) {
            Candle currentCandle = market.get(i);
            // Hole die passende Prediction aus der Liste, falls useMl aktiv ist
            MlPredictionResponse prediction = (useMl && !predictions.isEmpty()) 
                ? predictions.get(i - warmup) 
                : null;
            
            SignalDecision decision = signalEngine.generateSignal(market.subList(0, i + 1), prediction, useMl, indicators);

            // Handelsausführung simulieren
            if (decision.signal() == SignalType.BUY && currentQuantity == 0.0) {
                executeBuy(currentCandle);
            } else if (decision.signal() == SignalType.SELL && currentQuantity > 0.0) {
                executeSell(currentCandle);
            }
        }

        // Abschließende Auswertung: Schließe eventuell offene Positionen und berechne Metriken.
        Candle lastCandle = market.get(market.size() - 1);
        closeOpenPositionAtEnd(lastCandle);

        // Berechnung der Performance-Metriken
        double strategyReturnPct = ((currentCash - initialCapital) / initialCapital) * 100.0;
        double buyHoldReturnPct = ((lastCandle.close() - market.get(0).close()) / market.get(0).close()) * 100.0;
        double winRate = totalTradesCount == 0 ? 0.0 : (totalWins * 100.0 / totalTradesCount);

        // Rückgabe des Backtest-Ergebnisses.
        return new BacktestResult(strategyReturnPct, buyHoldReturnPct, currentCash - initialCapital, totalTradesCount, totalWins, totalLosses, winRate, market, new ArrayList<>(trades));
    }

    /**
     * Initialisiert die internen Variablen für einen neuen Simulations-Durchlauf.
     *
     * @param initialCapital Das Startkapital für die Simulation.
     */
    private void initSimulation(double initialCapital) {
        this.currentCash = initialCapital;
        this.currentQuantity = 0.0;
        this.currentEntryPrice = 0.0;
        this.totalWins = 0;
        this.totalLosses = 0;
        this.totalTradesCount = 0;
        this.trades = new ArrayList<>(); // Leere Liste für neue Trades
        this.tradeIdCounter = 1; // Startet die Trade-ID-Zählung neu
    }

    /**
     * Simuliert die Ausführung eines Kauf-Trades.
     * Berechnet die Positionsgröße und speichert den Trade.
     *
     * @param candle Die aktuelle Kerze, zu der der Kauf ausgeführt wird.
     */
    private void executeBuy(Candle candle) {
        // Berechnet die Menge basierend auf einem festen Prozentsatz des Kapitals (hier 1%).
        // Dies ist eine vereinfachte Positionsgrößenbestimmung.
        double buyQuantity = (currentCash * 0.01) / candle.close();
        currentQuantity = buyQuantity;
        currentEntryPrice = candle.close();
        
        // Speichert den Kauf-Trade.
        trades.add(new Trade(tradeIdCounter++, candle.timestamp(), SignalType.BUY, candle.close(), buyQuantity, 0.0, currentCash));
    }

    /**
     * Simuliert die Ausführung eines Verkauf-Trades.
     * Berechnet den Gewinn/Verlust, aktualisiert das Kapital und speichert den Trade.
     *
     * @param candle Die aktuelle Kerze, zu der der Verkauf ausgeführt wird.
     */
    private void executeSell(Candle candle) {
        // Berechnet den Gewinn oder Verlust (Profit and Loss) der geschlossenen Position.
        double pnl = (candle.close() - currentEntryPrice) * currentQuantity;
        currentCash += pnl; // Aktualisiert das virtuelle Kapital
        totalTradesCount++;
        
        // Aktualisiert die Zähler für gewonnene/verlorene Trades.
        if (pnl >= 0) totalWins++; else totalLosses++;
        
        // Speichert den Verkauf-Trade.
        trades.add(new Trade(tradeIdCounter++, candle.timestamp(), SignalType.SELL, candle.close(), currentQuantity, pnl, currentCash));
        
        // Setzt die offenen Positionsdaten zurück.
        currentQuantity = 0.0;
        currentEntryPrice = 0.0;
    }

    /**
     * Schließt eine eventuell am Ende des Backtests noch offene Position.
     * Dies stellt sicher, dass alle Trades in die finale Performance-Berechnung einfließen.
     *
     * @param lastCandle Die letzte Kerze der Marktdaten.
     */
    private void closeOpenPositionAtEnd(Candle lastCandle) {
        if (currentQuantity > 0.0) {
            log.info("Schließe offene Position am Ende des Backtests.");
            executeSell(lastCandle); // Nutzt die bestehende executeSell-Logik.
        }
    }
}
