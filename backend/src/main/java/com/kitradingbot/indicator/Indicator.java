package com.kitradingbot.indicator;

import com.kitradingbot.model.Candle;

import java.util.List;

/**
 * Basis-Interface für alle technischen Indikatoren im System.
 * Dieses Interface definiert die grundlegenden Methoden, die jeder Indikator
 * implementieren muss, um in der Handelsstrategie verwendet werden zu können.
 */
public interface Indicator {
    /**
     * Gibt den kanonischen Namen des Indikators zurück (z.B. "SMA", "RSI", "EMA").
     * Dieser Name wird für die Identifikation und Konfiguration des Indikators verwendet.
     *
     * @return Eine {@link String}-Repräsentation des Indikatornamens.
     */
    String name();

    /**
     * Gibt die minimale Anzahl der benötigten Perioden (Kerzen) zurück,
     * die für eine korrekte Berechnung des Indikators erforderlich sind.
     * Dies ist wichtig für die Bestimmung der "Warmup-Periode" des Systems.
     *
     * @return Die minimale Anzahl an Kerzen (als `int`), die für die Berechnung benötigt werden.
     */
    int requiredPeriods();

    /**
     * Berechnet den Wert des Indikators basierend auf einer Liste von Kerzen bis zu einem bestimmten Index.
     * Diese Methode verwendet typischerweise Standardparameter für die Berechnung.
     * Für Indikatoren, die dynamische Parameter unterstützen, sollte eine überladene Methode
     * mit zusätzlichen Parameter-Argumenten existieren.
     *
     * @param candles  Die {@link List} von {@link Candle}-Objekten, die die Marktdaten darstellen.
     * @param endIndex Der Index der Kerze in der Liste, für die der Indikatorwert berechnet werden soll.
     * @return Der berechnete Wert des Indikators als `double`.
     */
    double calculate(List<Candle> candles, int endIndex);
}
