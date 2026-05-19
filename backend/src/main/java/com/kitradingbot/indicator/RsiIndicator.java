package com.kitradingbot.indicator;

import com.kitradingbot.model.Candle;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Implementierung des Relative Strength Index (RSI) Indikators.
 * Der RSI ist ein Momentum-Oszillator, der die Geschwindigkeit und Veränderung
 * von Kursbewegungen misst. Er wird verwendet, um überkaufte oder überverkaufte
 * Zustände im Markt zu identifizieren.
 */
@Component
public class RsiIndicator implements Indicator {

    /**
     * Gibt den Namen des Indikators zurück.
     * @return Der String "RSI".
     */
    @Override
    public String name() {
        return "RSI";
    }

    /**
     * Gibt die standardmäßig benötigte Anzahl an Perioden für die RSI-Berechnung zurück.
     * Dies ist ein Fallback-Wert, da die Periode dynamisch übergeben werden kann.
     * @return Die standardmäßige Periodenanzahl (14).
     */
    @Override
    public int requiredPeriods() {
        return 14; // Standard-RSI-Periode
    }

    /**
     * Berechnet den RSI-Wert für die angegebene Kerzenliste bis zum Endindex mit einer Standardperiode von 14.
     * Diese Methode dient als Komfort-Wrapper für die detailliertere calculate-Methode.
     *
     * @param candles  Die Liste der Marktdaten (Kerzen).
     * @param endIndex Der Index der Kerze, für die der RSI berechnet werden soll.
     * @return Der berechnete RSI-Wert.
     */
    @Override
    public double calculate(List<Candle> candles, int endIndex) {
        return calculate(candles, endIndex, 14);
    }

    /**
     * Berechnet den RSI-Wert für die angegebene Kerzenliste bis zum Endindex mit einer dynamisch übergebenen Periode.
     *
     * @param candles  Die Liste der Marktdaten (Kerzen).
     * @param endIndex Der Index der Kerze, für die der RSI berechnet werden soll.
     * @param period   Die Periode, über die der RSI berechnet werden soll.
     * @return Der berechnete RSI-Wert (zwischen 0 und 100). Gibt 50.0 zurück, wenn nicht genügend Daten vorhanden sind.
     */
    public double calculate(List<Candle> candles, int endIndex, int period) {
        // Wenn nicht genügend Kerzen für die Berechnung vorhanden sind, einen neutralen Wert zurückgeben.
        // RSI benötigt 'period' Kerzen für die Berechnung der ersten RS-Werte, plus eine für die erste Differenz.
        if (endIndex < period) {
            return 50.0; // Neutraler Fallback-Wert
        }

        double gains = 0.0;
        double losses = 0.0;
        
        // Berechnung der durchschnittlichen Gewinne und Verluste über die angegebene Periode
        for (int i = endIndex - period + 1; i <= endIndex; i++) {
            // Die Differenz zum vorherigen Schlusskurs
            double change = candles.get(i).close() - candles.get(i - 1).close();
            if (change >= 0) {
                gains += change;
            } else {
                losses += Math.abs(change); // Absolute Verluste
            }
        }

        // Vermeidung von Division durch Null
        if (losses == 0.0) {
            return 100.0; // Wenn keine Verluste, ist der RSI 100 (stark überkauft)
        }

        double rs = gains / losses; // Relative Stärke
        return 100.0 - (100.0 / (1.0 + rs)); // RSI-Formel
    }
}
