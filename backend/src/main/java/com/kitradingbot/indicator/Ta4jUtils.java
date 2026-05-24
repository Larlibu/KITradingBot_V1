package com.kitradingbot.indicator;

import com.kitradingbot.model.Candle;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.num.DecimalNum;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Utility-Klasse zur Integration der ta4j-Bibliothek.
 * Diese Klasse bietet Hilfsmethoden, um interne Datenmodelle (wie Candle) in 
 * ta4j-kompatible Strukturen (wie BarSeries) umzuwandeln.
 */
public class Ta4jUtils {

    /**
     * Konvertiert eine Liste von Candle-Objekten in eine ta4j BarSeries.
     * Dies ist die Grundvoraussetzung, um technische Indikatoren aus ta4j auf unseren Marktdaten auszufuehren.
     *
     * @param candles Die Liste der historischen Marktdaten.
     * @param name    Der Name der zu erstellenden Serie (z.B. das Handelssymbol).
     * @return Eine vollstaendig initialisierte {@link BarSeries}.
     */
    public static BarSeries convertToBarSeries(List<Candle> candles, String name) {
        BarSeries series = new BaseBarSeriesBuilder().withName(name).withNumTypeOf(DecimalNum.class).build();
        for (Candle candle : candles) {
            ZonedDateTime zdt = candle.timestamp().atZone(ZoneId.systemDefault());
            series.addBar(zdt, candle.open(), candle.high(), candle.low(), candle.close(), candle.volume());
        }
        return series;
    }
}
