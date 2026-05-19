package com.kitradingbot.repository;

import com.kitradingbot.model.Candle;

import java.util.List;

/**
 * Interface für den Datenzugriff auf Markthistorien.
 * Definiert die Methoden zum Abrufen von historischen Marktdaten (Candlesticks).
 * In einer realen Anwendung würde dies die Kommunikation mit einer Börsen-API
 * oder einer Datenbank abstrahieren.
 */
public interface MarketDataRepository {
    /**
     * Ruft die neuesten Preiskerzen für ein bestimmtes Handelssymbol ab.
     * Die Daten werden in absteigender Reihenfolge des Zeitstempels zurückgegeben (neueste zuerst).
     *
     * @param symbol Das Handelspaar, für das die Daten abgerufen werden sollen (z.B. "BTCUSDT").
     * @param limit  Die maximale Anzahl der zurückzugebenden {@link Candle}-Objekte.
     * @return Eine {@link List} von {@link Candle}-Objekten, die die neuesten Marktdaten darstellen.
     */
    List<Candle> fetchRecent(String symbol, int limit);
}
