package com.kitradingbot.feature;

import com.kitradingbot.indicator.EmaIndicator;
import com.kitradingbot.indicator.RsiIndicator;
import com.kitradingbot.indicator.SmaIndicator;
import com.kitradingbot.model.Candle;
import com.kitradingbot.model.IndicatorConfig;
import com.kitradingbot.service.TradingBotException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Implementierung der {@link FeaturePipeline} fuer ML-Features.
 * Die Feature-Reihenfolge muss exakt mit dem Python-Training konsistent sein.
 */
@Component
public class IndicatorFeaturePipeline implements FeaturePipeline {
    private static final List<String> FEATURE_NAMES = List.of(
            "close",
            "sma",
            "rsi",
            "close_minus_sma",
            "return_1",
            "return_3",
            "volatility_6",
            "close_minus_ema"
    );

    private static final int DEFAULT_SMA_LEN = 20;
    private static final int DEFAULT_RSI_LEN = 14;
    private static final int DEFAULT_EMA_LEN = 21;
    private static final int VOLATILITY_WINDOW = 6;

    private final SmaIndicator smaIndicator;
    private final RsiIndicator rsiIndicator;
    private final EmaIndicator emaIndicator;

    public IndicatorFeaturePipeline(
            SmaIndicator smaIndicator,
            RsiIndicator rsiIndicator,
            EmaIndicator emaIndicator
    ) {
        this.smaIndicator = smaIndicator;
        this.rsiIndicator = rsiIndicator;
        this.emaIndicator = emaIndicator;
    }

    @Override
    public double[] buildFeatures(List<Candle> candles, List<IndicatorConfig> indicators) {
        if (candles == null || candles.isEmpty()) {
            throw new TradingBotException("Keine Marktdaten vorhanden, um den Feature-Vektor zu erstellen.");
        }

        int idx = candles.size() - 1;
        double close = candles.get(idx).close();

                // ML-Features muessen immer mit den trainierten Perioden berechnet werden,
        // unabhaengig von der Frontend-Anzeige.
        double sma = smaIndicator.calculate(candles, idx, DEFAULT_SMA_LEN);
        double rsi = rsiIndicator.calculate(candles, idx, DEFAULT_RSI_LEN);
        double ema = emaIndicator.calculate(candles, idx, DEFAULT_EMA_LEN);

        double return1 = calculateReturn(candles, idx, 1);
        double return3 = calculateReturn(candles, idx, 3);
        double volatility6 = calculateRollingVolatility(candles, idx, VOLATILITY_WINDOW);

        return new double[]{
                close,
                sma,
                rsi,
                close - sma,
                return1,
                return3,
                volatility6,
                close - ema
        };
    }

    @Override
    public List<String> featureNames() {
        return FEATURE_NAMES;
    }

    @Override
    public int warmupPeriod() {
        // Deckt SMA/RSI/EMA plus stabile Volatilitaetsberechnung ab.
        return 30;
    }

    private double calculateReturn(List<Candle> candles, int endIndex, int lookback) {
        if (endIndex < lookback) {
            return 0.0;
        }
        double previous = candles.get(endIndex - lookback).close();
        if (previous == 0.0) {
            return 0.0;
        }
        return (candles.get(endIndex).close() / previous) - 1.0;
    }

    private double calculateRollingVolatility(List<Candle> candles, int endIndex, int window) {
        int start = Math.max(1, endIndex - window + 1);
        int n = endIndex - start + 1;
        if (n <= 1) {
            return 0.0;
        }

        double sum = 0.0;
        double sumSq = 0.0;
        int count = 0;
        for (int i = start; i <= endIndex; i++) {
            double prevClose = candles.get(i - 1).close();
            if (prevClose == 0.0) {
                continue;
            }
            double r = (candles.get(i).close() / prevClose) - 1.0;
            sum += r;
            sumSq += r * r;
            count++;
        }

        if (count <= 1) {
            return 0.0;
        }
        double mean = sum / count;
        double variance = Math.max(0.0, (sumSq / count) - (mean * mean));
        return Math.sqrt(variance);
    }
}
