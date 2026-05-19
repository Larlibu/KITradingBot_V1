package com.kitradingbot.indicator;

import com.kitradingbot.model.Candle;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Implementierung des Simple Moving Average (SMA) Indikators.
 * Der SMA berechnet den arithmetischen Durchschnitt der Schlusskurse über eine
 * bestimmte Anzahl von Perioden. Er dient als Glättungsfilter für Preisdaten
 * und hilft, den Trend zu identifizieren.
 */
@Component
public class SmaIndicator implements Indicator {

    /**
     * Gibt den Namen des Indikators zurück.
     * @return Der String "SMA".
     */
    @Override
    public String name() {
        return "SMA";
    }

    /**
     * Gibt die standardmäßig benötigte Anzahl an Perioden für die SMA-Berechnung zurück.
     * Dies ist ein Fallback-Wert, da die Periode dynamisch übergeben werden kann.
     * @return Die standardmäßige Periodenanzahl (20).
     */
    @Override
    public int requiredPeriods() {
        return 20; // Standard-SMA-Periode
    }

    /**
     * Berechnet den SMA-Wert für die angegebene Kerzenliste bis zum Endindex mit einer Standardperiode von 20.
     * Diese Methode dient als Komfort-Wrapper für die detailliertere calculate-Methode.
     *
     * @param candles  Die Liste der Marktdaten (Kerzen).
     * @param endIndex Der Index der Kerze, für die der SMA berechnet werden soll.
     * @return Der berechnete SMA-Wert.
     */
    @Override
    public double calculate(List<Candle> candles, int endIndex) {
        // Fallback für Abwärtskompatibilität oder wenn keine spezifische Periode übergeben wird.
        return calculate(candles, endIndex, 20);
    }

    /**
     * Berechnet den SMA-Wert für die angegebene Kerzenliste bis zum Endindex mit einer dynamisch übergebenen Periode.
     *
     * @param candles  Die Liste der Marktdaten (Kerzen).
     * @param endIndex Der Index der Kerze, für die der SMA berechnet werden soll.
     * @param period   Die Periode, über die der SMA berechnet werden soll.
     * @return Der berechnete SMA-Wert. Gibt den Schlusskurs der aktuellen Kerze zurück, wenn nicht genügend Daten vorhanden sind.
     */
    public double calculate(List<Candle> candles, int endIndex, int period) {
        // Prüfen, ob genügend Daten für die Berechnung vorhanden sind.
        // Wenn nicht, den Schlusskurs der aktuellen Kerze als Fallback zurückgeben.
        if (endIndex < period - 1) {
            return candles.get(endIndex).close();
        }
        double sum = 0.0;
        // Summiert die Schlusskurse über die angegebene Periode.
        for (int i = endIndex - period + 1; i <= endIndex; i++) {
            sum += candles.get(i).close();
        }
        // Berechnet den Durchschnitt.
        return sum / period;
    }
}
