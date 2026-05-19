package com.kitradingbot.indicator;

import com.kitradingbot.model.Candle;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Implementierung des Exponential Moving Average (EMA) Indikators.
 * Der EMA ist ein gleitender Durchschnitt, der den jüngsten Datenpunkten eine höhere Gewichtung verleiht.
 * Er reagiert schneller auf Preisänderungen als ein einfacher gleitender Durchschnitt (SMA).
 */
@Component
public class EmaIndicator implements Indicator {
    /**
     * Gibt den Namen des Indikators zurück.
     * @return Der String "EMA".
     */
    @Override
    public String name() { return "EMA"; }

    /**
     * Gibt die standardmäßig benötigte Anzahl an Perioden für die EMA-Berechnung zurück.
     * Dies ist ein Fallback-Wert, da die Periode dynamisch übergeben werden kann.
     * @return Die standardmäßige Periodenanzahl (20).
     */
    @Override
    public int requiredPeriods() { return 20; }

    /**
     * Berechnet den EMA-Wert für die angegebene Kerzenliste bis zum Endindex mit einer Standardperiode von 20.
     * Diese Methode dient als Komfort-Wrapper für die detailliertere calculate-Methode.
     *
     * @param candles  Die Liste der Marktdaten (Kerzen).
     * @param endIndex Der Index der Kerze, für die der EMA berechnet werden soll.
     * @return Der berechnete EMA-Wert.
     */
    @Override
    public double calculate(List<Candle> candles, int endIndex) {
        return calculate(candles, endIndex, 20);
    }

    /**
     * Berechnet den EMA-Wert für die angegebene Kerzenliste bis zum Endindex mit einer dynamisch übergebenen Periode.
     *
     * @param candles  Die Liste der Marktdaten (Kerzen).
     * @param endIndex Der Index der Kerze, für die der EMA berechnet werden soll.
     * @param period   Die Periode, über die der EMA berechnet werden soll.
     * @return Der berechnete EMA-Wert. Gibt den Schlusskurs der aktuellen Kerze zurück, wenn nicht genügend Daten vorhanden sind.
     */
    public double calculate(List<Candle> candles, int endIndex, int period) {
        // Wenn nicht genügend Kerzen für die Berechnung vorhanden sind, den aktuellen Schlusskurs zurückgeben.
        if (endIndex < period - 1) return candles.get(endIndex).close();

        // Berechnung des Glättungsfaktors (Multiplier)
        double k = 2.0 / (period + 1);

        // Initialer EMA-Wert (oft als SMA der ersten 'period' Kerzen oder einfach der ersten Kerze der Periode)
        // Hier wird der Schlusskurs der ersten Kerze der Periode als Startwert verwendet.
        double ema = candles.get(endIndex - period + 1).close();

        // Iteration über die restlichen Kerzen der Periode, um den EMA zu berechnen
        for (int i = endIndex - period + 2; i <= endIndex; i++) {
            ema = candles.get(i).close() * k + ema * (1 - k);
        }
        return ema;
    }
}
