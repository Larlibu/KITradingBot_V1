package com.kitradingbot.backtesting;

import com.kitradingbot.feature.FeaturePipeline;
import com.kitradingbot.indicator.Ta4jUtils;
import com.kitradingbot.integration.MlServiceClient;
import com.kitradingbot.model.*;
import com.kitradingbot.service.MarketDataService;
import com.kitradingbot.signal.MlRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.ta4j.core.*;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.rules.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SimpleBacktestEngine implements BacktestEngine {
    private static final Logger log = LoggerFactory.getLogger(SimpleBacktestEngine.class);
    private final MarketDataService marketDataService;
    private final FeaturePipeline featurePipeline;
    private final MlServiceClient mlServiceClient;

    public SimpleBacktestEngine(MarketDataService marketDataService, FeaturePipeline featurePipeline, MlServiceClient mlServiceClient) {
        this.marketDataService = marketDataService;
        this.featurePipeline = featurePipeline;
        this.mlServiceClient = mlServiceClient;
    }

    @Override
    public BacktestResult run(String symbol, int candles, boolean useMl, List<IndicatorConfig> indicators) {
        List<Candle> market = marketDataService.getMarketData(symbol, candles);
        if (market.isEmpty()) return null;

        BarSeries series = Ta4jUtils.convertToBarSeries(market, symbol);
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        Rule entry = null; Rule exit = null;
        Map<String, org.ta4j.core.Indicator<org.ta4j.core.num.Num>> ta4jInds = new HashMap<>();

        if (indicators != null) {
            for (IndicatorConfig config : indicators) {
                Rule iEntry = null; Rule iExit = null;
                String type = config.type().toUpperCase();
                
                if ("SMA".equals(type)) {
                    int len = config.getIntParam("length", 20);
                    SMAIndicator sma = new SMAIndicator(closePrice, len);
                    ta4jInds.put("SMA " + len, sma);
                    iEntry = new CrossedUpIndicatorRule(closePrice, sma);
                    iExit = new CrossedDownIndicatorRule(closePrice, sma);
                } else if ("RSI".equals(type)) {
                    int len = config.getIntParam("length", 14);
                    double low = config.getDoubleParam("oversold", 30);
                    double high = config.getDoubleParam("overbought", 70);
                    RSIIndicator rsi = new RSIIndicator(closePrice, len);
                    ta4jInds.put("RSI " + len, rsi);
                    iEntry = new UnderIndicatorRule(rsi, series.numOf(low));
                    iExit = new OverIndicatorRule(rsi, series.numOf(high));
                } else if ("EMA".equals(type)) {
                    int len = config.getIntParam("length", 20);
                    EMAIndicator ema = new EMAIndicator(closePrice, len);
                    ta4jInds.put("EMA " + len, ema);
                    iEntry = new CrossedUpIndicatorRule(closePrice, ema);
                    iExit = new CrossedDownIndicatorRule(closePrice, ema);
                } else if ("BBANDS".equals(type)) {
                    int len = config.getIntParam("length", 20);
                    double m = config.getDoubleParam("mult", 2.0);
                    StandardDeviationIndicator sd = new StandardDeviationIndicator(closePrice, len);
                    BollingerBandsMiddleIndicator mid = new BollingerBandsMiddleIndicator(new SMAIndicator(closePrice, len));
                    BollingerBandsLowerIndicator lo = new BollingerBandsLowerIndicator(mid, sd, series.numOf(m));
                    BollingerBandsUpperIndicator up = new BollingerBandsUpperIndicator(mid, sd, series.numOf(m));
                    ta4jInds.put("BB Upper", up); ta4jInds.put("BB Lower", lo);
                    iEntry = new UnderIndicatorRule(closePrice, lo);
                    iExit = new OverIndicatorRule(closePrice, up);
                }

                if (iEntry != null) {
                    entry = (entry == null) ? iEntry : new AndRule(entry, iEntry);
                    exit = (exit == null) ? iExit : new AndRule(exit, iExit);
                }
            }
        }

        if (entry == null) entry = new BooleanRule(false);
        if (exit == null) exit = new BooleanRule(false);

        List<MlPredictionResponse> predictions = new ArrayList<>();
        int warmup = 35;
        if (useMl) {
            List<double[]> feats = new ArrayList<>();
            for (int i = warmup; i < market.size(); i++) feats.add(featurePipeline.buildFeatures(market.subList(0, i + 1), indicators));
            try { 
                predictions = mlServiceClient.predictBatch(feats); 
                Rule mlBuy = new MlRule(predictions, SignalType.BUY, 0.5, warmup);
                Rule mlSell = new MlRule(predictions, SignalType.SELL, 0.5, warmup);
                entry = new AndRule(entry, mlBuy);
                exit = new AndRule(exit, mlSell);
            } catch (Exception e) { 
                log.error("ML Prediction failed", e);
                useMl = false; 
            }
        }

        Strategy strategy = new BaseStrategy(entry, exit);
        TradingRecord record = new BaseTradingRecord();
        List<SignalDecision> decisions = new ArrayList<>();
        List<List<IndicatorValue>> indHistory = new ArrayList<>();

        for (int i = 0; i < series.getBarCount(); i++) {
            List<IndicatorValue> stepInds = new ArrayList<>();
            for (var e : ta4jInds.entrySet()) {
                try { stepInds.add(new IndicatorValue(e.getKey(), e.getValue().getValue(i).doubleValue())); } catch (Exception ex) {}
            }
            indHistory.add(stepInds);

            if (i < warmup) {
                decisions.add(new SignalDecision(SignalType.HOLD, "Warmup", 0, market.get(i).timestamp()));
                continue;
            }

            SignalType sig = SignalType.HOLD;
            if (strategy.shouldEnter(i, record)) { 
                sig = SignalType.BUY; 
                record.enter(i, series.getBar(i).getClosePrice(), series.numOf(1)); 
            } else if (strategy.shouldExit(i, record)) { 
                sig = SignalType.SELL; 
                record.exit(i, series.getBar(i).getClosePrice(), series.numOf(1)); 
            }
            
            double confidence = 0;
            if (useMl && (i - warmup >= 0) && (i - warmup < predictions.size())) {
                confidence = predictions.get(i - warmup).confidence();
            }
            
            decisions.add(new SignalDecision(sig, "ta4j", confidence, market.get(i).timestamp()));
        }

        return convertToBacktestResult(series, record, market, decisions, indHistory);
    }

    private BacktestResult convertToBacktestResult(BarSeries s, TradingRecord r, List<Candle> m, List<SignalDecision> d, List<List<IndicatorValue>> h) {
        List<com.kitradingbot.model.Trade> tradesList = new ArrayList<>();
        double cap = 10000.0;
        for (Position p : r.getPositions()) {
            double p1 = p.getEntry().getNetPrice().doubleValue();
            double p2 = p.getExit().getNetPrice().doubleValue();
            cap += (p2 - p1);
            tradesList.add(new com.kitradingbot.model.Trade(0, m.get(p.getEntry().getIndex()).timestamp(), SignalType.BUY, p1, 1.0, 0, 0));
            tradesList.add(new com.kitradingbot.model.Trade(0, m.get(p.getExit().getIndex()).timestamp(), SignalType.SELL, p2, 1.0, p2-p1, cap));
        }
        return new BacktestResult(((cap-10000)/100), 0, cap-10000, r.getPositionCount(), 0, 0, 0, m, tradesList, d, h);
    }
}
