package com.kitradingbot.service;

import com.kitradingbot.model.Candle;

import java.util.List;

/**
 * Interface für den Zugriff auf Marktdaten.
 * Definiert die Schnittstelle für Services, die historische Marktdaten (Candlesticks)
 * bereitstellen.
 */
public interface MarketDataService {
    /**
     * Ruft historische Kerzendaten für ein bestimmtes Handelssymbol ab.
     * Die Daten werden typischerweise in absteigender Reihenfolge des Zeitstempels zurückgegeben (neueste zuerst).
     *
     * @param symbol Das Handelssymbol, für das die Daten abgerufen werden sollen (z.B. "BTCUSDT").
     * @param limit  Die maximale Anzahl der abzurufenden Kerzen.
     * @return Eine {@link List} von {@link Candle}-Objekten, die die historischen Marktdaten darstellen.
     * @throws TradingBotException wenn die Daten nicht abgerufen werden können oder Parameter ungültig sind.
     */
    List<Candle> getMarketData(String symbol, int limit);
}
