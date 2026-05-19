package com.kitradingbot.indicator;

import com.kitradingbot.model.Candle;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Implementierung des Bollinger Bands Indikators.
 * Bollinger Bänder sind ein Volatilitätskanal, der um einen einfachen gleitenden Durchschnitt (SMA) gelegt wird.
 * Sie bestehen aus drei Linien: einem mittleren Band (SMA) und einem oberen und unteren Band,
 * die typischerweise zwei Standardabweichungen vom mittleren Band entfernt sind.
 */
@Component
public class BbandsIndicator {

    /**
     * Ein Record zur Speicherung der drei Bollinger Bänder.
     *
     * @param upper  Der Wert des oberen Bollinger Bandes.
     * @param lower  Der Wert des unteren Bollinger Bandes.
     * @param middle Der Wert des mittleren Bollinger Bandes (SMA).
     */
    public record Bands(double upper, double lower, double middle) {}

    /**
     * Berechnet die Bollinger Bänder für eine gegebene Kerzenliste bis zu einem bestimmten Endindex.
     *
     * @param candles  Die Liste der Marktdaten (Kerzen).
     * @param endIndex Der Index der Kerze, für die die Bänder berechnet werden sollen.
     * @param period   Die Periode für den gleitenden Durchschnitt (SMA) und die Standardabweichung.
     * @param mult     Der Multiplikator für die Standardabweichung, um die oberen und unteren Bänder zu bestimmen (z.B. 2.0 für 2 Standardabweichungen).
     * @return Ein {@link Bands}-Record, das die Werte für das obere, untere und mittlere Band enthält.
     *         Gibt bei unzureichenden Daten Bänder zurück, die alle auf dem Schlusskurs der aktuellen Kerze liegen.
     */
    public Bands calculate(List<Candle> candles, int endIndex, int period, double mult) {
        // Prüfen, ob genügend Daten für die Berechnung vorhanden sind.
        // Wenn nicht, wird der aktuelle Schlusskurs als Fallback für alle Bänder verwendet.
        if (endIndex < period - 1) {
            double close = candles.get(endIndex).close();
            return new Bands(close, close, close);
        }

        // 1. Berechnung des mittleren Bandes (SMA)
        double sum = 0;
        for (int i = endIndex - period + 1; i <= endIndex; i++) {
            sum += candles.get(i).close();
        }
        double mean = sum / period; // Dies ist das mittlere Band

        // 2. Berechnung der Standardabweichung
        double sqSum = 0; // Summe der quadrierten Abweichungen
        for (int i = endIndex - period + 1; i <= endIndex; i++) {
            sqSum += Math.pow(candles.get(i).close() - mean, 2);
        }
        double stdDev = Math.sqrt(sqSum / period); // Standardabweichung

        // 3. Berechnung der oberen und unteren Bänder
        return new Bands(mean + stdDev * mult, mean - stdDev * mult, mean);
    }
}
