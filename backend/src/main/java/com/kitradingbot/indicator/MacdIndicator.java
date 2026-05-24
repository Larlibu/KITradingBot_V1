package com.kitradingbot.indicator;

import com.kitradingbot.model.Candle;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import java.util.List;

/**
 * Implementierung des Moving Average Convergence Divergence (MACD) Indikators mittels ta4j.
 */
@Component
public class MacdIndicator implements Indicator {

    public record MacdValue(double macd, double signal, double histogram) {}

    @Override
    public String name() {
        return "MACD";
    }

    @Override
    public int requiredPeriods() {
        return 26 + 9; // Typical MACD requirements
    }

    @Override
    public double calculate(List<Candle> candles, int endIndex) {
        // Default calculate returns the MACD line value
        return calculateMacd(candles, endIndex, 12, 26, 9).macd();
    }

    public MacdValue calculateMacd(List<Candle> candles, int endIndex, int fastPeriod, int slowPeriod, int signalPeriod) {
        if (candles == null || candles.isEmpty() || endIndex < slowPeriod) {
            return new MacdValue(0, 0, 0);
        }

        BarSeries series = Ta4jUtils.convertToBarSeries(candles.subList(0, endIndex + 1), "MACD_Series");
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        MACDIndicator macd = new MACDIndicator(closePrice, fastPeriod, slowPeriod);
        EMAIndicator signal = new EMAIndicator(macd, signalPeriod);

        int lastIdx = series.getEndIndex();
        double macdVal = macd.getValue(lastIdx).doubleValue();
        double signalVal = signal.getValue(lastIdx).doubleValue();
        double histogramVal = macdVal - signalVal;

        return new MacdValue(macdVal, signalVal, histogramVal);
    }
}
