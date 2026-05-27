package com.kitradingbot.signal;

import com.kitradingbot.model.MlPredictionResponse;
import com.kitradingbot.model.SignalType;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.rules.AbstractRule;

import java.util.Collections;
import java.util.List;

/**
 * Eine benutzerdefinierte ta4j-Regel, die auf Machine-Learning-Vorhersagen basiert.
 * Dies erlaubt es, die KI-Logik nahtlos in die ta4j-Handelsstrategien zu integrieren.
 */
public class MlRule extends AbstractRule {
    private final List<MlPredictionResponse> predictions;
    private final SignalType targetSignal;
    private final double confidenceThreshold;
    private final int warmupOffset;

    public MlRule(MlPredictionResponse prediction, SignalType targetSignal, double confidenceThreshold) {
        this(prediction != null ? Collections.singletonList(prediction) : Collections.emptyList(), 
             targetSignal, confidenceThreshold, 0);
    }

    public MlRule(List<MlPredictionResponse> predictions, SignalType targetSignal, double confidenceThreshold, int warmupOffset) {
        this.predictions = predictions;
        this.targetSignal = targetSignal;
        this.confidenceThreshold = confidenceThreshold;
        this.warmupOffset = warmupOffset;
    }

    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        if (predictions == null || predictions.isEmpty()) return false;

        int predIndex;
        if (predictions.size() == 1) {
            predIndex = 0;
        } else {
            predIndex = index - warmupOffset;
        }

        if (predIndex < 0 || predIndex >= predictions.size()) return false;
        
        MlPredictionResponse prediction = predictions.get(predIndex);
        if (prediction == null) return false;

        boolean confident = prediction.confidence() >= confidenceThreshold;
        String label = prediction.label().toUpperCase();

        if (targetSignal == SignalType.BUY) {
            return confident && (label.contains("UP") || label.contains("STEIGT"));
        } else if (targetSignal == SignalType.SELL) {
            return confident && (label.contains("DOWN") || label.contains("FAELLT"));
        }
        return false;
    }
}
