package com.kitradingbot.indicator;

import com.kitradingbot.model.Candle;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import java.util.List;

/**
 * Implementierung des Relative Strength Index (RSI) Indikators mittels ta4j.
 */
@Component
public class RsiIndicator implements Indicator {

    @Override
    public String name() {
        return "RSI";
    }

    @Override
    public int requiredPeriods() {
        return 14;
    }

    @Override
    public double calculate(List<Candle> candles, int endIndex) {
        return calculate(candles, endIndex, 14);
    }

    public double calculate(List<Candle> candles, int endIndex, int period) {
        if (candles == null || candles.isEmpty() || endIndex < 0) {
            return 50.0;
        }
        if (endIndex < period) {
            return 50.0;
        }

        BarSeries series = Ta4jUtils.convertToBarSeries(candles.subList(0, endIndex + 1), "RSI_Series");
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        RSIIndicator rsi = new RSIIndicator(closePrice, period);

        return rsi.getValue(series.getEndIndex()).doubleValue();
    }
}
