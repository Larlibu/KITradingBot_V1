package com.kitradingbot.indicator;

import com.kitradingbot.model.Candle;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import java.util.List;

/**
 * Implementierung des Simple Moving Average (SMA) Indikators mittels ta4j.
 */
@Component
public class SmaIndicator implements Indicator {

    @Override
    public String name() {
        return "SMA";
    }

    @Override
    public int requiredPeriods() {
        return 20;
    }

    @Override
    public double calculate(List<Candle> candles, int endIndex) {
        return calculate(candles, endIndex, 20);
    }

    public double calculate(List<Candle> candles, int endIndex, int period) {
        if (candles == null || candles.isEmpty() || endIndex < 0) {
            return 0.0;
        }
        if (endIndex < period - 1) {
            return candles.get(endIndex).close();
        }

        BarSeries series = Ta4jUtils.convertToBarSeries(candles.subList(0, endIndex + 1), "SMA_Series");
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator sma = new SMAIndicator(closePrice, period);

        return sma.getValue(series.getEndIndex()).doubleValue();
    }
}
