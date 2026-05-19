package com.kitradingbot.repository;

import com.kitradingbot.model.Trade;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-Memory Implementierung des TradeRepository.
 * Speichert Trades in einer thread-sicheren Liste im Arbeitsspeicher.
 * Nach einem Neustart der Anwendung sind diese Daten verloren.
 */
@Repository
public class InMemoryTradeRepository implements TradeRepository {
    /** Generator für fortlaufende IDs. */
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    /** Thread-sichere Liste zur Speicherung der Trades. */
    private final CopyOnWriteArrayList<Trade> trades = new CopyOnWriteArrayList<>();

    @Override
    public long nextId() {
        return idGenerator.getAndIncrement();
    }

    @Override
    public Trade save(Trade trade) {
        trades.add(trade);
        return trade;
    }

    /**
     * Gibt eine Kopie der Liste zurück, um externe Modifikationen zu verhindern.
     */
    @Override
    public List<Trade> findAll() {
        return List.copyOf(trades);
    }

    @Override
    public void clear() {
        trades.clear();
    }
}
