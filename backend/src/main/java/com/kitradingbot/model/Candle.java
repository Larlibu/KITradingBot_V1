package com.kitradingbot.model;

import java.time.LocalDateTime;

/**
 * Repräsentiert eine einzelne Kurskerze (Candlestick) in einem bestimmten Zeitintervall.
 * Dieses Record-Objekt speichert die Open, High, Low, Close (OHLC) Preise
 * sowie das gehandelte Volumen für eine spezifische Zeitperiode.
 *
 * @param timestamp Der genaue Zeitpunkt, zu dem die Kerze geöffnet wurde.
 * @param open      Der Eröffnungskurs des Assets zu Beginn des Zeitintervalls der Kerze.
 * @param high      Der höchste Kurs, der während des Zeitintervalls der Kerze erreicht wurde.
 * @param low       Der niedrigste Kurs, der während des Zeitintervalls der Kerze erreicht wurde.
 * @param close     Der Schlusskurs des Assets am Ende des Zeitintervalls der Kerze.
 * @param volume    Das gehandelte Volumen des Assets während des Zeitintervalls der Kerze.
 */
public record Candle(
        LocalDateTime timestamp,
        double open,
        double high,
        double low,
        double close,
        double volume
) {
}
