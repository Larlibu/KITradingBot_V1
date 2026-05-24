package com.kitradingbot.signal;

import com.kitradingbot.model.MlPredictionResponse;
import com.kitradingbot.model.SignalType;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.rules.AbstractRule;

/**
 * Eine benutzerdefinierte ta4j-Regel, die auf Machine-Learning-Vorhersagen basiert.
 * Dies erlaubt es, die KI-Logik nahtlos in die ta4j-Handelsstrategien zu integrieren.
 */
public class MlRule extends AbstractRule {
    /** Die vom ML-Service empfangene Vorhersage (z.B. Kurs steigt/faellt). */
    private final MlPredictionResponse prediction;
    /** Das Signal (BUY oder SELL), auf das diese Regel pruefen soll. */
    private final SignalType targetSignal;
    /** Der Schwellenwert fuer die Konfidenz der Vorhersage (0.0 bis 1.0). */
    private final double confidenceThreshold;

    /**
     * Erstellt eine neue ML-Regel.
     *
     * @param prediction         Die aktuelle KI-Vorhersage.
     * @param targetSignal       Das zu validierende Signal (BUY fuer Long, SELL fuer Short).
     * @param confidenceThreshold Die Mindestkonfidenz, ab der die Regel als erfuellt gilt.
     */
    public MlRule(MlPredictionResponse prediction, SignalType targetSignal, double confidenceThreshold) {
        this.prediction = prediction;
        this.targetSignal = targetSignal;
        this.confidenceThreshold = confidenceThreshold;
    }

    /**
     * Prueft, ob die KI-Vorhersage die Bedingungen fuer das Zielsignal erfuellt.
     *
     * @param index         Der aktuelle Bar-Index in der BarSeries.
     * @param tradingRecord Der aktuelle Status der Handelsausfuehrung.
     * @return true, wenn die Vorhersage konfident genug ist und dem Zielsignal entspricht.
     */
    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
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
