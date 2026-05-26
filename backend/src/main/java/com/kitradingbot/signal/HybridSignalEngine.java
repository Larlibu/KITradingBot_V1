package com.kitradingbot.signal;

import com.kitradingbot.config.TradingProperties;
import com.kitradingbot.indicator.Ta4jUtils;
import com.kitradingbot.model.Candle;
import com.kitradingbot.model.IndicatorConfig;
import com.kitradingbot.model.MlPredictionResponse;
import com.kitradingbot.model.SignalDecision;
import com.kitradingbot.model.SignalType;
import org.springframework.stereotype.Component;
import org.ta4j.core.*;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.rules.*;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class HybridSignalEngine implements SignalEngine {
    private final TradingProperties props;
    public HybridSignalEngine(TradingProperties props) { this.props = props; }

    @Override
    public SignalDecision generateSignal(List<Candle> candles, MlPredictionResponse pred, boolean useMl, List<IndicatorConfig> indicators) {
        if (candles.isEmpty()) return new SignalDecision(SignalType.HOLD, "Keine Daten", 0, LocalDateTime.now());
        BarSeries series = Ta4jUtils.convertToBarSeries(candles, "Live");
        int idx = series.getEndIndex();
        
        Strategy strat = buildStrategy(series, pred, indicators, useMl);
        TradingRecord record = new BaseTradingRecord(); 
        
        SignalType sig = SignalType.HOLD;
        if (strat.shouldEnter(idx, record)) sig = SignalType.BUY;
        else if (strat.shouldExit(idx, record)) sig = SignalType.SELL;

        return new SignalDecision(sig, "ta4j", pred != null ? pred.confidence() : 0, LocalDateTime.now());
    }

    private Strategy buildStrategy(BarSeries series, MlPredictionResponse pred, List<IndicatorConfig> indicators, boolean useMl) {
        ClosePriceIndicator close = new ClosePriceIndicator(series);
        Rule entry = null; Rule exit = null;

        if (indicators != null) {
            for (IndicatorConfig config : indicators) {
                Rule iEntry = null; Rule iExit = null;
                String type = config.type().toUpperCase();
                
                if ("SMA".equals(type)) {
                    int len = config.getIntParam("length", 20);
                    SMAIndicator sma = new SMAIndicator(close, len);
                    iEntry = new CrossedUpIndicatorRule(close, sma);
                    iExit = new CrossedDownIndicatorRule(close, sma);
                } else if ("RSI".equals(type)) {
                    int len = config.getIntParam("length", 14);
                    double lo = config.getDoubleParam("oversold", 30);
                    double hi = config.getDoubleParam("overbought", 70);
                    RSIIndicator rsi = new RSIIndicator(close, len);
                    // MEHR SIGNALE: Under/Over
                    iEntry = new UnderIndicatorRule(rsi, series.numOf(lo));
                    iExit = new OverIndicatorRule(rsi, series.numOf(hi));
                } else if ("EMA".equals(type)) {
                    int len = config.getIntParam("length", 20);
                    EMAIndicator ema = new EMAIndicator(close, len);
                    iEntry = new CrossedUpIndicatorRule(close, ema);
                    iExit = new CrossedDownIndicatorRule(close, ema);
                } else if ("BBANDS".equals(type)) {
                    int len = config.getIntParam("length", 20);
                    double m = config.getDoubleParam("mult", 2.0);
                    StandardDeviationIndicator sd = new StandardDeviationIndicator(close, len);
                    BollingerBandsMiddleIndicator mid = new BollingerBandsMiddleIndicator(new SMAIndicator(close, len));
                    iEntry = new UnderIndicatorRule(close, new BollingerBandsLowerIndicator(mid, sd, series.numOf(m)));
                    iExit = new OverIndicatorRule(close, new BollingerBandsUpperIndicator(mid, sd, series.numOf(m)));
                }

                if (iEntry != null) {
                    entry = (entry == null) ? iEntry : new AndRule(entry, iEntry);
                    exit = (exit == null) ? iExit : new AndRule(exit, iExit);
                }
            }
        }

        if (useMl && pred != null) {
            double thr = props.getMlConfidenceThreshold();
            entry = (entry == null) ? new MlRule(pred, SignalType.BUY, thr) : new AndRule(entry, new MlRule(pred, SignalType.BUY, thr));
            exit = (exit == null) ? new MlRule(pred, SignalType.SELL, thr) : new AndRule(exit, new MlRule(pred, SignalType.SELL, thr));
        }

        return new BaseStrategy(entry != null ? entry : new BooleanRule(false), exit != null ? exit : new BooleanRule(false));
    }
}
