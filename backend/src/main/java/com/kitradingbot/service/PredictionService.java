package com.kitradingbot.service;

import com.kitradingbot.model.MlPredictionResponse;

/**
 * Interface für den Vorhersage-Dienst.
 * Kapselt den Zugriff auf Machine-Learning-Modelle und deren Management.
 * Dieser Service dient als Abstraktionsschicht zum externen ML-Service.
 */
public interface PredictionService {
    /**
     * Berechnet eine Preisprognose basierend auf einem gegebenen Feature-Vektor.
     * Diese Methode kommuniziert mit dem externen ML-Service, um die Vorhersage zu erhalten.
     *
     * @param features Ein `double`-Array, das den Feature-Vektor für die Vorhersage darstellt (z.B. Schlusskurs, SMA, RSI).
     * @return Ein {@link MlPredictionResponse}-Objekt mit dem prognostizierten Label und der Konfidenz.
     * @throws TradingBotException wenn die Vorhersage fehlschlägt oder der ML-Service nicht erreichbar ist.
     */
    MlPredictionResponse predict(double[] features);

    /**
     * Löst manuell einen neuen Trainingsvorgang für das ML-Modell im externen ML-Service aus.
     * Das Training läuft typischerweise asynchron.
     *
     * @throws TradingBotException wenn der Trainingsvorgang nicht ausgelöst werden kann.
     */
    void triggerMlTraining();
}
