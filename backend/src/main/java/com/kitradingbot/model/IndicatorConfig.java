package com.kitradingbot.model;

import java.util.Map;

/**
 * Repräsentiert die Konfiguration eines technischen Indikators,
 * die vom Frontend an das Backend übertragen wird.
 * Dieses Record-Objekt ermöglicht die dynamische Konfiguration von Indikatoren
 * im Backend basierend auf Benutzereingaben im Frontend.
 *
 * @param type   Der Typ des Indikators (z.B. "SMA", "RSI", "EMA", "BBANDS").
 * @param label  Ein menschenlesbarer Name oder Label für den Indikator.
 * @param params Eine Map, die die spezifischen Parameter des Indikators enthält (z.B. "length", "overbought", "mult").
 *               Die Werte in der Map können verschiedene Typen haben, daher `Object`.
 */
public record IndicatorConfig(
    String type,
    String label,
    Map<String, Object> params
) {
    /**
     * Hilfsmethode zum sicheren Abrufen eines numerischen Parameters als `double` aus der `params`-Map.
     * Konvertiert den Wert sicher und bietet einen Fallback-Wert.
     *
     * @param key          Der Schlüssel des Parameters in der `params`-Map.
     * @param defaultValue Der Standardwert, der zurückgegeben wird, wenn der Parameter nicht gefunden wird oder nicht konvertiert werden kann.
     * @return Der Parameterwert als `double` oder der `defaultValue`.
     */
    public double getDoubleParam(String key, double defaultValue) {
        if (params == null || !params.containsKey(key)) return defaultValue;
        Object val = params.get(key);
        if (val instanceof Number n) return n.doubleValue(); // Direkte Konvertierung, wenn es bereits eine Zahl ist
        try {
            return Double.parseDouble(val.toString()); // Versuch der String-Konvertierung
        } catch (Exception e) {
            return defaultValue; // Bei Fehler den Standardwert zurückgeben
        }
    }

    /**
     * Hilfsmethode zum sicheren Abrufen eines numerischen Parameters als `int` aus der `params`-Map.
     * Nutzt {@link #getDoubleParam(String, double)} und castet das Ergebnis zu `int`.
     *
     * @param key          Der Schlüssel des Parameters in der `params`-Map.
     * @param defaultValue Der Standardwert, der zurückgegeben wird, wenn der Parameter nicht gefunden wird oder nicht konvertiert werden kann.
     * @return Der Parameterwert als `int` oder der `defaultValue`.
     */
    public int getIntParam(String key, int defaultValue) {
        return (int) getDoubleParam(key, defaultValue);
    }
}
