package com.kitradingbot.service;

import com.kitradingbot.model.Candle;
import com.kitradingbot.repository.MarketDataRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementierung des {@link MarketDataService}.
 * Diese Service-Klasse kapselt die Geschäftslogik für den Abruf von Marktdaten
 * und nutzt ein {@link MarketDataRepository} zur eigentlichen Datenbeschaffung.
 */
@Service
public class MarketDataServiceImpl implements MarketDataService {
    private final MarketDataRepository marketDataRepository;

    /**
     * Konstruktor für den MarketDataServiceImpl.
     * Spring injiziert automatisch das benötigte {@link MarketDataRepository}.
     *
     * @param marketDataRepository Das Repository für den Zugriff auf Marktdaten.
     */
    public MarketDataServiceImpl(MarketDataRepository marketDataRepository) {
        this.marketDataRepository = marketDataRepository;
    }

    /**
     * Ruft die neuesten Marktdaten (Candlesticks) für ein bestimmtes Symbol ab.
     * Führt eine grundlegende Validierung der Eingabeparameter durch.
     *
     * @param symbol Das Handelspaar, für das die Daten abgerufen werden sollen (z.B. "BTCUSDT").
     * @param limit  Die maximale Anzahl der zurückzugebenden Kerzen.
     * @return Eine {@link List} von {@link Candle}-Objekten, die die neuesten Marktdaten darstellen.
     * @throws TradingBotException wenn das Symbol leer ist oder das Limit ungültig ist.
     */
    @Override
    public List<Candle> getMarketData(String symbol, int limit) {
        if (symbol == null || symbol.isBlank()) {
            throw new TradingBotException("Symbol darf nicht leer sein.");
        }
        if (limit <= 0) {
            throw new TradingBotException("Limit muss größer als Null sein.");
        }
        // Ruft die Daten über das Repository ab.
        return marketDataRepository.fetchRecent(symbol, limit);
    }
}
