package com.kitradingbot.model;

/**
 * Repräsentiert die Antwort des Machine Learning Service nach einer Vorhersage.
 * Dieses Record-Objekt enthält das vorhergesagte Label und die Konfidenz des Modells.
 *
 * @param label      Das vorhergesagte Label (z.B. "STEIGT", "FÄLLT").
 * @param confidence Die Konfidenz des Modells in diese Vorhersage (Wert zwischen 0.0 und 1.0).
 */
public record MlPredictionResponse(String label, double confidence) {
}
