package com.kitradingbot.signal;

import com.kitradingbot.config.TradingProperties;
import com.kitradingbot.indicator.RsiIndicator;
import com.kitradingbot.indicator.SmaIndicator;
import com.kitradingbot.indicator.EmaIndicator;
import com.kitradingbot.indicator.BbandsIndicator;
import com.kitradingbot.model.Candle;
import com.kitradingbot.model.IndicatorConfig;
import com.kitradingbot.model.MlPredictionResponse;
import com.kitradingbot.model.SignalDecision;
import com.kitradingbot.model.SignalType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Eine hybride Implementierung der {@link SignalEngine}, die technische Indikatoren
 * mit Machine-Learning-Vorhersagen kombiniert.
 * Diese Engine generiert ein Handelssignal (BUY, SELL, HOLD) basierend auf einem Konsens
 * zwischen den konfigurierten Indikatoren und optional der KI-Vorhersage.
 * Die Indikatoren können dynamisch vom Frontend konfiguriert werden.
 */
@Component
public class HybridSignalEngine implements SignalEngine {
    private final TradingProperties properties;
    private final RsiIndicator rsiIndicator;
    private final SmaIndicator smaIndicator;
    private final EmaIndicator emaIndicator;
    private final BbandsIndicator bbandsIndicator;

    /**
     * Konstruktor für die HybridSignalEngine.
     * Spring injiziert automatisch alle benötigten Abhängigkeiten.
     *
     * @param properties      Die globalen Trading-Konfigurationen.
     * @param rsiIndicator    Der {@link RsiIndicator} zur Berechnung des RSI.
     * @param smaIndicator    Der {@link SmaIndicator} zur Berechnung des SMA.
     * @param emaIndicator    Der {@link EmaIndicator} zur Berechnung des EMA.
     * @param bbandsIndicator Der {@link BbandsIndicator} zur Berechnung der Bollinger Bänder.
     */
    public HybridSignalEngine(
            TradingProperties properties,
            RsiIndicator rsiIndicator,
            SmaIndicator smaIndicator,
            EmaIndicator emaIndicator,
            BbandsIndicator bbandsIndicator
    ) {
        this.properties = properties;
        this.rsiIndicator = rsiIndicator;
        this.smaIndicator = smaIndicator;
        this.emaIndicator = emaIndicator;
        this.bbandsIndicator = bbandsIndicator;
    }

    /**
     * Generiert eine Signalentscheidung basierend auf den aktuellen Marktdaten,
     * einer optionalen KI-Vorhersage und einer Liste dynamisch konfigurierter Indikatoren.
     * Die Logik prüft, ob ein Konsens für ein Kauf- oder Verkaufssignal besteht.
     *
     * @param candles    Die Liste der historischen Kerzen, wobei die letzte Kerze die aktuelle ist.
     * @param prediction Die Vorhersage des Machine-Learning-Modells.
     * @param useMl      Ein Flag, das angibt, ob die KI-Vorhersage in die Konsensbildung einbezogen werden soll.
     * @param indicators Eine Liste von {@link IndicatorConfig}-Objekten, die die vom Benutzer
     *                   konfigurierten Indikatoren und deren Parameter enthalten.
     * @return Eine {@link SignalDecision}, die das generierte Signal, eine Begründung und die Konfidenz enthält.
     */
    @Override
    public SignalDecision generateSignal(List<Candle> candles, MlPredictionResponse prediction, boolean useMl, List<IndicatorConfig> indicators) {
        int idx = candles.size() - 1; // Index der aktuellsten Kerze
        double close = candles.get(idx).close(); // Schlusskurs der aktuellen Kerze
        List<String> statuses = new ArrayList<>(); // Für die detaillierte Begründung

        // 1. KI-Vorhersage prüfen (nur wenn useMl aktiv ist)
        boolean mlConfirmsBuy = true; // Annahme: KI stimmt zu, wenn nicht aktiv oder kein klares Signal
        boolean mlConfirmsSell = true; // Annahme: KI stimmt zu, wenn nicht aktiv oder kein klares Signal

        if (useMl && prediction != null) {
            boolean confident = prediction.confidence() >= properties.getMlConfidenceThreshold();
            boolean isUp = "STEIGT".equalsIgnoreCase(prediction.label());
            boolean isDown = "FÄLLT".equalsIgnoreCase(prediction.label());

            // KI bestätigt nur, wenn sie konfident ist und das entsprechende Signal gibt
            mlConfirmsBuy = confident && isUp;
            mlConfirmsSell = confident && isDown;

            statuses.add("KI=" + (isUp ? "BUY" : (isDown ? "SELL" : "HOLD")));
        } else if (useMl && prediction == null) {
            // Wenn KI aktiv, aber keine Prediction verfügbar, dann kein Konsens durch KI
            mlConfirmsBuy = false;
            mlConfirmsSell = false;
            statuses.add("KI=N/A");
        }


        // 2. Dynamische Indikatoren auswerten
        boolean allIndicatorsBuy = true; // Annahme: Alle Indikatoren stimmen einem Kauf zu
        boolean allIndicatorsSell = true; // Annahme: Alle Indikatoren stimmen einem Verkauf zu
        
        // Wenn Indikatoren vom Frontend übergeben wurden, diese auswerten
        if (indicators != null && !indicators.isEmpty()) {
            for (IndicatorConfig config : indicators) {
                String type = config.type().toUpperCase();
                
                // SMA-Logik: Kauf, wenn Close > SMA; Verkauf, wenn Close < SMA
                if ("SMA".equals(type)) {
                    int len = config.getIntParam("length", 20);
                    double val = smaIndicator.calculate(candles, idx, len);
                    if (close <= val) allIndicatorsBuy = false; // Close nicht über SMA -> kein Kauf
                    if (close >= val) allIndicatorsSell = false; // Close nicht unter SMA -> kein Verkauf
                    statuses.add("SMA=" + (close > val ? "BUY" : (close < val ? "SELL" : "HOLD")));
                } 
                // EMA-Logik: Kauf, wenn Close > EMA; Verkauf, wenn Close < EMA
                else if ("EMA".equals(type)) {
                    int len = config.getIntParam("length", 20);
                    double val = emaIndicator.calculate(candles, idx, len);
                    if (close <= val) allIndicatorsBuy = false;
                    if (close >= val) allIndicatorsSell = false;
                    statuses.add("EMA=" + (close > val ? "BUY" : (close < val ? "SELL" : "HOLD")));
                } 
                // RSI-Logik: Kauf, wenn RSI <= Oversold; Verkauf, wenn RSI >= Overbought
                else if ("RSI".equals(type)) {
                    int len = config.getIntParam("length", 14);
                    double ob = config.getDoubleParam("overbought", 70);
                    double os = config.getDoubleParam("oversold", 30);
                    double val = rsiIndicator.calculate(candles, idx, len);
                    if (val > os) allIndicatorsBuy = false; // RSI nicht unter Oversold -> kein Kauf
                    if (val < ob) allIndicatorsSell = false; // RSI nicht über Overbought -> kein Verkauf
                    statuses.add("RSI=" + (val <= os ? "BUY" : (val >= ob ? "SELL" : "HOLD")));
                } 
                // BBANDS-Logik: Kauf, wenn Close < Lower Band; Verkauf, wenn Close > Upper Band
                else if ("BBANDS".equals(type)) {
                    BbandsIndicator.Bands bands = bbandsIndicator.calculate(candles, idx, config.getIntParam("length", 20), config.getDoubleParam("mult", 2.0));
                    if (close >= bands.lower()) allIndicatorsBuy = false; // Close nicht unter Lower Band -> kein Kauf
                    if (close <= bands.upper()) allIndicatorsSell = false; // Close nicht über Upper Band -> kein Verkauf
                    statuses.add("BB=" + (close < bands.lower() ? "BUY" : (close > bands.upper() ? "SELL" : "HOLD")));
                }
                // Hier könnten weitere Indikatoren hinzugefügt werden (z.B. MACD)
            }
        } else {
            // Fallback-Logik: Wenn keine Indikatoren vom Frontend übergeben wurden,
            // werden die Standard-Indikatoren aus den TradingProperties verwendet.
            // Dies ist primär für Abwärtskompatibilität oder einfache Tests.
            double rsi = rsiIndicator.calculate(candles, idx, properties.getRsiPeriod());
            double sma = smaIndicator.calculate(candles, idx, properties.getSmaPeriod());
            
            // Standard-RSI-Logik
            boolean rsiBuy = rsi <= properties.getRsiOversold();
            boolean rsiSell = rsi >= properties.getRsiOverbought();
            
            // Standard-SMA-Logik
            boolean smaBuy = close > sma;
            boolean smaSell = close < sma;

            allIndicatorsBuy = rsiBuy && smaBuy;
            allIndicatorsSell = rsiSell && smaSell;

            statuses.add("RSI=" + (rsiBuy ? "BUY" : (rsiSell ? "SELL" : "HOLD")));
            statuses.add("SMA=" + (smaBuy ? "BUY" : (smaSell ? "SELL" : "HOLD")));
        }

        // 3. Finale Entscheidung: Konsens-Logik
        // Ein Signal wird nur generiert, wenn alle relevanten Bedingungen erfüllt sind.
        SignalType signal = SignalType.HOLD;
        if (allIndicatorsBuy && mlConfirmsBuy) {
            signal = SignalType.BUY;
        } else if (allIndicatorsSell && mlConfirmsSell) {
            signal = SignalType.SELL;
        }

        // Erstellung der Begründung für die Entscheidung
        String reason = (signal != SignalType.HOLD) 
            ? "Konsens erreicht: " + signal 
            : "Kein Konsens: " + String.join(", ", statuses);

        // Rückgabe der Signalentscheidung
        return new SignalDecision(signal, reason, prediction != null ? prediction.confidence() : 0, LocalDateTime.now());
    }
}
