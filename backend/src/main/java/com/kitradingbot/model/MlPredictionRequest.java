package com.kitradingbot.model;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Repräsentiert eine Anfrage an den Machine Learning Service für eine Vorhersage.
 * Dieses Record-Objekt wird verwendet, um einen Feature-Vektor an den ML-Service zu senden.
 *
 * @param features Eine {@link List} von `Double`-Werten, die den Feature-Vektor für die Vorhersage darstellt.
 *                 Die Liste darf nicht leer sein (validiert durch `@NotEmpty`).
 */
public record MlPredictionRequest(@NotEmpty List<Double> features) {
}
