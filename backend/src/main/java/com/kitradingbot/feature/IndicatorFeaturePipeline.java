package com.kitradingbot.feature;

import com.kitradingbot.indicator.EmaIndicator;
import com.kitradingbot.indicator.RsiIndicator;
import com.kitradingbot.indicator.SmaIndicator;
import com.kitradingbot.model.Candle;
import com.kitradingbot.model.IndicatorConfig;
import com.kitradingbot.service.TradingBotException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Optimierte Feature-Pipeline, die UI-Parameter beruecksichtigt.
 */
@Component
public class IndicatorFeaturePipeline implements FeaturePipeline {
    private static final List<String> FEATURE_NAMES = List.of("close", "sma", "rsi", "close_minus_sma", "return_1", "return_3", "volatility_6", "close_minus_ema");

    private final SmaIndicator smaIndicator;
    private final RsiIndicator rsiIndicator;
    private final EmaIndicator emaIndicator;

    public IndicatorFeaturePipeline(SmaIndicator smaIndicator, RsiIndicator rsiIndicator, EmaIndicator emaIndicator) {
        this.smaIndicator = smaIndicator;
        this.rsiIndicator = rsiIndicator;
        this.emaIndicator = emaIndicator;
    }

    @Override
    public double[] buildFeatures(List<Candle> candles, List<IndicatorConfig> indicators) {
        if (candles == null || candles.isEmpty()) throw new TradingBotException("Keine Daten");

        int idx = candles.size() - 1;
        double close = candles.get(idx).close();

        // Parameter aus der UI suchen, sonst Defaults
        int smaLen = findParam(indicators, "SMA", "length", 20);
        int rsiLen = findParam(indicators, "RSI", "length", 14);
        int emaLen = findParam(indicators, "EMA", "length", 21);

        double sma = smaIndicator.calculate(candles, idx, smaLen);
        double rsi = rsiIndicator.calculate(candles, idx, rsiLen);
        double ema = emaIndicator.calculate(candles, idx, emaLen);

        return new double[]{
                close, sma, rsi, close - sma,
                calculateReturn(candles, idx, 1),
                calculateReturn(candles, idx, 3),
                calculateRollingVolatility(candles, idx, 6),
                close - ema
        };
    }

    private int findParam(List<IndicatorConfig> configs, String type, String key, int def) {
        if (configs == null) return def;
        return configs.stream()
                .filter(c -> c.type().equalsIgnoreCase(type))
                .findFirst()
                .map(c -> c.getIntParam(key, def))
                .orElse(def);
    }

    @Override
    public List<String> featureNames() { return FEATURE_NAMES; }
    @Override
    public int warmupPeriod() { return 35; }

    private double calculateReturn(List<Candle> candles, int idx, int lookback) {
        if (idx < lookback) return 0.0;
        double prev = candles.get(idx - lookback).close();
        return prev == 0 ? 0 : (candles.get(idx).close() / prev) - 1.0;
    }

    private double calculateRollingVolatility(List<Candle> candles, int idx, int window) {
        int start = Math.max(1, idx - window + 1);
        if (idx - start + 1 <= 1) return 0.0;
        double sum = 0, sumSq = 0;
        int count = 0;
        for (int i = start; i <= idx; i++) {
            double r = (candles.get(i).close() / candles.get(i-1).close()) - 1.0;
            sum += r; sumSq += r * r; count++;
        }
        double mean = sum / count;
        return Math.sqrt(Math.max(0, (sumSq / count) - (mean * mean)));
    }
}
