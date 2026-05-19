package com.kitradingbot.model;

import java.util.List;

/**
 * Repräsentiert einen umfassenden Schnappschuss des aktuellen Zustands der Signalanalyse.
 * Dieses Record-Objekt wird verwendet, um dem Frontend alle relevanten Daten für die
 * aktuelle Handelsentscheidung anzuzeigen, einschließlich Marktdaten, KI-Vorhersage
 * und der finalen Strategieentscheidung.
 *
 * @param currentPrice   Der aktuellste Schlusskurs des Assets, für das die Analyse durchgeführt wurde.
 * @param prediction     Das {@link MlPredictionResponse}-Objekt, das die Vorhersage des Machine-Learning-Modells enthält.
 * @param decision       Das {@link SignalDecision}-Objekt, das die finale Handelsentscheidung der Strategie darstellt.
 * @param tradingEnabled Ein Flag, das angibt, ob der automatisierte Live-Handel aktuell aktiviert ist.
 * @param featureNames   Eine {@link List} von Strings, die die Namen der Features (Indikatoren) enthält, die für die KI-Vorhersage verwendet wurden.
 * @param features       Ein `double`-Array, das die konkreten numerischen Werte der Features zum aktuellen Zeitpunkt enthält.
 * @param indicatorValues Eine {@link List} von {@link IndicatorValue}-Objekten, die die berechneten Werte der
 *                        vom Benutzer konfigurierten Indikatoren enthält.
 */
public record SignalSnapshot(
        double currentPrice,
        MlPredictionResponse prediction,
        SignalDecision decision,
        boolean tradingEnabled,
        List<String> featureNames,
        double[] features,
        List<IndicatorValue> indicatorValues
) {
}
