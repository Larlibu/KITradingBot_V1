package com.kitradingbot.model;

/**
 * Repräsentiert den berechneten Wert eines Indikators zu einem bestimmten Zeitpunkt.
 * Wird verwendet, um die Werte der vom Benutzer konfigurierten Indikatoren im Frontend anzuzeigen.
 *
 * @param name  Der Name des Indikators (z.B. "SMA 20", "RSI 14").
 * @param value Der berechnete numerische Wert des Indikators.
 */
public record IndicatorValue(String name, double value) {
}
