package com.kitradingbot.indicator;

import com.kitradingbot.model.Candle;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;

import java.util.List;

/**
 * Implementierung des Bollinger Bands Indikators mittels ta4j.
 */
@Component
public class BbandsIndicator {

    public record Bands(double upper, double lower, double middle) {}

    public Bands calculate(List<Candle> candles, int endIndex, int period, double mult) {
        if (candles == null || candles.isEmpty() || endIndex < 0) {
             return new Bands(0, 0, 0);
        }
        if (endIndex < period - 1) {
            double close = candles.get(endIndex).close();
            return new Bands(close, close, close);
        }

        BarSeries series = Ta4jUtils.convertToBarSeries(candles.subList(0, endIndex + 1), "BB_Series");
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator sma = new SMAIndicator(closePrice, period);
        StandardDeviationIndicator stdDev = new StandardDeviationIndicator(closePrice, period);
        
        BollingerBandsMiddleIndicator middleBB = new BollingerBandsMiddleIndicator(sma);
        BollingerBandsLowerIndicator lowerBB = new BollingerBandsLowerIndicator(middleBB, stdDev, series.numOf(mult));
        BollingerBandsUpperIndicator upperBB = new BollingerBandsUpperIndicator(middleBB, stdDev, series.numOf(mult));

        int lastIdx = series.getEndIndex();
        return new Bands(
                upperBB.getValue(lastIdx).doubleValue(),
                lowerBB.getValue(lastIdx).doubleValue(),
                middleBB.getValue(lastIdx).doubleValue()
        );
    }
}
