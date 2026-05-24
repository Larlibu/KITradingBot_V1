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
import org.ta4j.core.rules.AndRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Optimierte Backtest-Engine, die ta4j fuer die Simulation nutzt.
 * Berechnet Indikatoren und Signale zentral im Backend, um Konsistenz zu garantieren.
 */
@Component
public class SimpleBacktestEngine implements BacktestEngine {
    private static final Logger log = LoggerFactory.getLogger(SimpleBacktestEngine.class);
    
    private final MarketDataService marketDataService;
    private final FeaturePipeline featurePipeline;
    private final MlServiceClient mlServiceClient;

    public SimpleBacktestEngine(
            MarketDataService marketDataService,
            FeaturePipeline featurePipeline,
            MlServiceClient mlServiceClient
    ) {
        this.marketDataService = marketDataService;
        this.featurePipeline = featurePipeline;
        this.mlServiceClient = mlServiceClient;
    }

    @Override
    public BacktestResult run(String symbol, int candles, boolean useMl, List<IndicatorConfig> indicators) {
        log.info("Starte ta4j Backtest fuer {}", symbol);
        List<Candle> market = marketDataService.getMarketData(symbol, candles);
        BarSeries series = Ta4jUtils.convertToBarSeries(market, symbol);
        
        // ML Batch Predictions
        List<MlPredictionResponse> predictions = new ArrayList<>();
        int warmup = featurePipeline.warmupPeriod();
        if (useMl) {
            List<double[]> allFeatures = new ArrayList<>();
            for (int i = warmup; i < market.size(); i++) {
                allFeatures.add(featurePipeline.buildFeatures(market.subList(0, i + 1), indicators));
            }
            try {
                predictions = mlServiceClient.predictBatch(allFeatures);
            } catch (Exception e) {
                log.error("ML Batch Prediction failed", e);
                useMl = false;
            }
        }

        // ta4j Simulation
        TradingRecord tradingRecord = new BaseTradingRecord();
        List<SignalDecision> decisions = new ArrayList<>();
        List<List<IndicatorValue>> indicatorHistory = new ArrayList<>();
        
        // Vorbereiten der ta4j Indikatoren fuer die Historie
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        Map<String, org.ta4j.core.Indicator<org.ta4j.core.num.Num>> ta4jInds = new HashMap<>();
        
        for (IndicatorConfig config : indicators) {
            String type = config.type().toUpperCase();
            if ("SMA".equals(type)) {
                int len = config.getIntParam("length", 20);
                ta4jInds.put("SMA " + len, new SMAIndicator(closePrice, len));
            } else if ("EMA".equals(type)) {
                int len = config.getIntParam("length", 20);
                ta4jInds.put("EMA " + len, new EMAIndicator(closePrice, len));
            } else if ("RSI".equals(type)) {
                int len = config.getIntParam("length", 14);
                ta4jInds.put("RSI " + len, new RSIIndicator(closePrice, len));
            } else if ("BBANDS".equals(type)) {
                int len = config.getIntParam("length", 20);
                double mult = config.getDoubleParam("mult", 2.0);
                SMAIndicator sma = new SMAIndicator(closePrice, len);
                StandardDeviationIndicator sd = new StandardDeviationIndicator(closePrice, len);
                BollingerBandsMiddleIndicator middle = new BollingerBandsMiddleIndicator(sma);
                ta4jInds.put("BB Middle", middle);
                ta4jInds.put("BB Upper", new BollingerBandsUpperIndicator(middle, sd, series.numOf(mult)));
                ta4jInds.put("BB Lower", new BollingerBandsLowerIndicator(middle, sd, series.numOf(mult)));
            } else if ("MACD".equals(type)) {
                int fast = config.getIntParam("fast", 12);
                int slow = config.getIntParam("slow", 26);
                int signal = config.getIntParam("signal", 9);
                MACDIndicator macd = new MACDIndicator(closePrice, fast, slow);
                ta4jInds.put("MACD", macd);
                ta4jInds.put("MACD Signal", new EMAIndicator(macd, signal));
            }
        }

        double initialCapital = 10000.0;
        
        for (int i = 0; i < series.getBarCount(); i++) {
            List<IndicatorValue> stepInds = new ArrayList<>();
            for (Map.Entry<String, org.ta4j.core.Indicator<org.ta4j.core.num.Num>> entry : ta4jInds.entrySet()) {
                try {
                    stepInds.add(new IndicatorValue(entry.getKey(), entry.getValue().getValue(i).doubleValue()));
                } catch (Exception e) {
                    // Falls Indikator fuer diesen Index noch nicht berechenbar (Warmup)
                }
            }
            indicatorHistory.add(stepInds);

            if (i < warmup) {
                decisions.add(new SignalDecision(SignalType.HOLD, "Warmup", 0, market.get(i).timestamp()));
                continue;
            }

            MlPredictionResponse prediction = (useMl && (i - warmup < predictions.size())) ? predictions.get(i - warmup) : null;
            Strategy strategy = buildStrategy(series, prediction, indicators, useMl);
            
            SignalType signal = SignalType.HOLD;
            if (strategy.shouldEnter(i, tradingRecord)) {
                signal = SignalType.BUY;
                tradingRecord.enter(i, series.getBar(i).getClosePrice(), series.numOf(1));
            } else if (strategy.shouldExit(i, tradingRecord)) {
                signal = SignalType.SELL;
                tradingRecord.exit(i, series.getBar(i).getClosePrice(), series.numOf(1));
            }
            decisions.add(new SignalDecision(signal, "ta4j simulation", prediction != null ? prediction.confidence() : 0, market.get(i).timestamp()));
        }

        return convertToBacktestResult(series, tradingRecord, initialCapital, market, decisions, indicatorHistory);
    }

    private Strategy buildStrategy(BarSeries series, MlPredictionResponse prediction, List<IndicatorConfig> indicators, boolean useMl) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        Rule entryRule = null;
        Rule exitRule = null;

        for (IndicatorConfig config : indicators) {
            Rule iEntry = null;
            Rule iExit = null;
            String type = config.type().toUpperCase();
            if ("SMA".equals(type)) {
                int len = config.getIntParam("length", 20);
                SMAIndicator sma = new SMAIndicator(closePrice, len);
                iEntry = new OverIndicatorRule(closePrice, sma);
                iExit = new UnderIndicatorRule(closePrice, sma);
            } else if ("RSI".equals(type)) {
                int len = config.getIntParam("length", 14);
                RSIIndicator rsi = new RSIIndicator(closePrice, len);
                iEntry = new UnderIndicatorRule(rsi, config.getDoubleParam("oversold", 30));
                iExit = new OverIndicatorRule(rsi, config.getDoubleParam("overbought", 70));
            } else if ("EMA".equals(type)) {
                int len = config.getIntParam("length", 20);
                EMAIndicator ema = new EMAIndicator(closePrice, len);
                iEntry = new OverIndicatorRule(closePrice, ema);
                iExit = new UnderIndicatorRule(closePrice, ema);
            }
            
            if (iEntry != null) {
                entryRule = (entryRule == null) ? iEntry : new AndRule(entryRule, iEntry);
                exitRule = (exitRule == null) ? iExit : new AndRule(exitRule, iExit);
            }
        }

        if (useMl && prediction != null) {
            MlRule mlBuy = new MlRule(prediction, SignalType.BUY, 0.5);
            MlRule mlSell = new MlRule(prediction, SignalType.SELL, 0.5);
            entryRule = (entryRule == null) ? mlBuy : new AndRule(entryRule, mlBuy);
            exitRule = (exitRule == null) ? mlSell : new AndRule(exitRule, mlSell);
        }

        if (entryRule == null) entryRule = new org.ta4j.core.rules.BooleanRule(false);
        if (exitRule == null) exitRule = new org.ta4j.core.rules.BooleanRule(false);

        return new BaseStrategy(entryRule, exitRule);
    }

    private BacktestResult convertToBacktestResult(BarSeries series, TradingRecord record, double initialCapital, List<Candle> market, List<SignalDecision> decisions, List<List<IndicatorValue>> indicatorHistory) {
        List<com.kitradingbot.model.Trade> trades = new ArrayList<>();
        double currentCapital = initialCapital;
        int wins = 0;
        int losses = 0;

        for (Position pos : record.getPositions()) {
            double entryPrice = pos.getEntry().getNetPrice().doubleValue();
            double exitPrice = pos.getExit().getNetPrice().doubleValue();
            double pnl = (exitPrice - entryPrice) * 1.0; 
            currentCapital += pnl;
            if (pnl >= 0) wins++; else losses++;

            trades.add(new com.kitradingbot.model.Trade(0, market.get(pos.getEntry().getIndex()).timestamp(), SignalType.BUY, entryPrice, 1.0, 0, 0));
            trades.add(new com.kitradingbot.model.Trade(0, market.get(pos.getExit().getIndex()).timestamp(), SignalType.SELL, exitPrice, 1.0, pnl, currentCapital));
        }

        double strategyReturn = ((currentCapital - initialCapital) / initialCapital) * 100.0;
        double buyHold = ((series.getLastBar().getClosePrice().doubleValue() - series.getFirstBar().getClosePrice().doubleValue()) / series.getFirstBar().getClosePrice().doubleValue()) * 100.0;

        return new BacktestResult(strategyReturn, buyHold, currentCapital - initialCapital, record.getPositionCount(), wins, losses, record.getPositionCount() == 0 ? 0 : (wins * 100.0 / record.getPositionCount()), market, trades, decisions, indicatorHistory);
    }
}
