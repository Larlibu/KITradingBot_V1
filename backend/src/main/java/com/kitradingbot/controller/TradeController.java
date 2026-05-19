package com.kitradingbot.controller;

import com.kitradingbot.model.Trade;
import com.kitradingbot.trading.TradingEngine;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST-Controller für die Verwaltung von Trades und den Status des Live-Handelsbots.
 * Ermöglicht das Starten, Stoppen und Zurücksetzen des automatisierten Handels
 * sowie das Abrufen der Historie der ausgeführten Trades.
 */
@RestController
@RequestMapping("/api/trades")
public class TradeController {
    private final TradingEngine tradingEngine;

    /**
     * Konstruktor für den TradeController.
     * Spring injiziert automatisch die benötigte {@link TradingEngine}.
     *
     * @param tradingEngine Die Engine, die den Live-Handel simuliert und verwaltet.
     */
    public TradeController(TradingEngine tradingEngine) {
        this.tradingEngine = tradingEngine;
    }

    /**
     * Ruft eine Liste aller durchgeführten Trades seit dem letzten Reset oder Start des Bots ab.
     *
     * @return Eine {@link List} von {@link Trade}-Objekten, die die Historie der ausgeführten Trades darstellt.
     */
    @GetMapping
    public List<Trade> getTrades() {
        return tradingEngine.getTrades();
    }

    /**
     * Startet den automatisierten Live-Handel des Bots.
     * Nach dem Aufruf beginnt die {@link TradingEngine}, Signale zu verarbeiten und Trades auszuführen.
     *
     * @return Eine Map mit dem aktuellen Status des Handels (z.B. "started", "tradingEnabled": true).
     */
    @PostMapping("/start")
    public Map<String, Object> startTrading() {
        tradingEngine.start();
        return Map.of("status", "started", "tradingEnabled", true);
    }

    /**
     * Stoppt den automatisierten Live-Handel des Bots.
     * Der Bot wird keine weiteren Trades ausführen, bis er erneut gestartet wird.
     * Offene Positionen bleiben bestehen, werden aber nicht automatisch geschlossen.
     *
     * @return Eine Map mit dem aktuellen Status des Handels (z.B. "stopped", "tradingEnabled": false).
     */
    @PostMapping("/stop")
    public Map<String, Object> stopTrading() {
        tradingEngine.stop();
        return Map.of("status", "stopped", "tradingEnabled", false);
    }

    /**
     * Setzt die Handels-Statistiken und die Historie der Trades zurück.
     * Dies löscht alle aufgezeichneten Trades und setzt das Kapital auf den Initialwert zurück.
     * Der Handelsstatus (gestartet/gestoppt) bleibt dabei unverändert.
     *
     * @return Eine Map mit dem aktuellen Status nach dem Reset.
     */
    @PostMapping("/reset")
    public Map<String, Object> resetTrading() {
        tradingEngine.reset();
        return Map.of("status", "reset", "tradingEnabled", tradingEngine.isRunning());
    }
}
