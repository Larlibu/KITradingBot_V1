package com.kitradingbot.signal;

import com.kitradingbot.config.TradingProperties;
import com.kitradingbot.indicator.Ta4jUtils;
import com.kitradingbot.model.Candle;
import com.kitradingbot.model.IndicatorConfig;
import com.kitradingbot.model.MlPredictionResponse;
import com.kitradingbot.model.SignalDecision;
import com.kitradingbot.model.SignalType;
import org.springframework.stereotype.Component;
import org.ta4j.core.*;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.AndRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Hybride Signal-Engine, die ta4j-Strategien nutzt, um Kauf- und Verkaufssignale zu generieren.
 * Sie kombiniert klassische technische Indikatoren mit modernen Machine-Learning-Vorhersagen
 * in einer einheitlichen ta4j-Strategie-Logik.
 */
@Component
public class HybridSignalEngine implements SignalEngine {
    /** Globale Trading-Konfigurationen, z.B. Schwellenwerte fuer KI-Konfidenz. */
    private final TradingProperties properties;

    /**
     * Konstruktor fuer die HybridSignalEngine.
     *
     * @param properties Das Konfigurationsobjekt fuer Trading-Einstellungen.
     */
    public HybridSignalEngine(TradingProperties properties) {
        this.properties = properties;
    }

    /**
     * Generiert eine Signalentscheidung basierend auf der aktuellen Marktphase.
     * Nutzt ta4j-Regeln, um Indikatoren und ML-Vorhersagen zu evaluieren.
     *
     * @param candles    Die aktuellen Marktdaten (Zeitreihe der Kerzen).
     * @param prediction Die Vorhersage des ML-Modells fuer den aktuellen Zeitpunkt.
     * @param useMl      Flag zur Aktivierung oder Deaktivierung der KI-Logik.
     * @param indicators Liste der dynamisch konfigurierten Indikatoren vom Dashboard.
     * @return Eine {@link SignalDecision} mit dem berechneten Signal (BUY, SELL oder HOLD).
     */
    @Override
    public SignalDecision generateSignal(List<Candle> candles, MlPredictionResponse prediction, boolean useMl, List<IndicatorConfig> indicators) {
        // Konvertierung in ta4j-Format fuer die Strategie-Auswertung
        BarSeries series = Ta4jUtils.convertToBarSeries(candles, "LiveSeries");
        int lastIdx = series.getEndIndex();
        
        // Dynamischer Aufbau der Strategie
        Strategy strategy = buildStrategy(series, prediction, indicators, useMl);
        TradingRecord tradingRecord = new BaseTradingRecord(); 
        
        // Evaluation der Ein- und Ausstiegsregeln am aktuellsten Datenpunkt
        SignalType signal = SignalType.HOLD;
        if (strategy.shouldEnter(lastIdx, tradingRecord)) {
            signal = SignalType.BUY;
        } else if (strategy.shouldExit(lastIdx, tradingRecord)) {
            signal = SignalType.SELL;
        }

        return new SignalDecision(signal, "ta4j Strategy Evaluated", prediction != null ? prediction.confidence() : 0, LocalDateTime.now());
    }

    /**
     * Baut eine ta4j-Handelsstrategie dynamisch aus den Frontend-Konfigurationen und KI-Vorhersagen zusammen.
     *
     * @param series     Die aktuelle Kurs-Zeitreihe.
     * @param prediction Die KI-Vorhersage.
     * @param indicators Die Liste der konfigurierten Indikatoren.
     * @param useMl      Gibt an, ob KI in die Regeln aufgenommen werden soll.
     * @return Eine evaluierbare {@link Strategy}.
     */
    private Strategy buildStrategy(BarSeries series, MlPredictionResponse prediction, List<IndicatorConfig> indicators, boolean useMl) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        Rule entryRule = null;
        Rule exitRule = null;

        // Iteration ueber alle vom Benutzer konfigurierten Indikatoren
        for (IndicatorConfig config : indicators) {
            Rule iEntry = null;
            Rule iExit = null;
            String type = config.type().toUpperCase();
            
            if ("SMA".equals(type)) {
                int len = config.getIntParam("length", 20);
                SMAIndicator sma = new SMAIndicator(closePrice, len);
                iEntry = new OverIndicatorRule(closePrice, sma);
                iExit = new UnderIndicatorRule(closePrice, sma);
            } else if ("RSI".equals(type)) {
                int len = config.getIntParam("length", 14);
                RSIIndicator rsi = new RSIIndicator(closePrice, len);
                iEntry = new UnderIndicatorRule(rsi, config.getDoubleParam("oversold", 30));
                iExit = new OverIndicatorRule(rsi, config.getDoubleParam("overbought", 70));
            }
            
            // Verknuepfung der Regeln (Standard: Alle Bedingungen muessen erfuellt sein -> AndRule)
            if (iEntry != null) {
                entryRule = (entryRule == null) ? iEntry : new AndRule(entryRule, iEntry);
                exitRule = (exitRule == null) ? iExit : new AndRule(exitRule, iExit);
            }
        }

        // Integration der KI-Logik als zusaetzliche Filter-Regel
        if (useMl && prediction != null) {
            double threshold = properties.getMlConfidenceThreshold();
            MlRule mlBuy = new MlRule(prediction, SignalType.BUY, threshold);
            MlRule mlSell = new MlRule(prediction, SignalType.SELL, threshold);
            entryRule = (entryRule == null) ? mlBuy : new AndRule(entryRule, mlBuy);
            exitRule = (exitRule == null) ? mlSell : new AndRule(exitRule, mlSell);
        }

        // Fallback: Wenn keine Regeln definiert sind, passiert nichts (HOLD)
        if (entryRule == null) entryRule = new org.ta4j.core.rules.BooleanRule(false);
        if (exitRule == null) exitRule = new org.ta4j.core.rules.BooleanRule(false);

        return new BaseStrategy(entryRule, exitRule);
    }
}
