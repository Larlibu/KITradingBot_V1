package com.kitradingbot.repository;

import com.kitradingbot.model.Trade;

import java.util.List;

/**
 * Interface für den Zugriff auf ausgeführte Trades.
 */
public interface TradeRepository {
    /**
     * @return Die nächste freie ID für einen neuen Trade.
     */
    long nextId();

    /**
     * Speichert einen neuen Trade dauerhaft oder im Speicher.
     * @param trade Der zu speichernde Trade.
     * @return Der gespeicherte Trade.
     */
    Trade save(Trade trade);

    /**
     * @return Eine Liste aller gespeicherten Trades.
     */
    List<Trade> findAll();

    /**
     * Löscht alle Trades aus dem Repository.
     */
    void clear();
}
