package com.kitradingbot.trading;

import com.kitradingbot.model.Candle;
import com.kitradingbot.model.SignalDecision;
import com.kitradingbot.model.Trade;

import java.util.List;

/**
 * Interface für die Trading-Engine, die für die Ausführung von Handelsaufträgen zuständig ist.
 * Dieses Interface abstrahiert die Logik für die Platzierung und Verwaltung von Trades,
 * sei es für den Live-Handel an einer Börse oder für Simulationen (Paper Trading).
 */
public interface TradingEngine {
    /**
     * Verarbeitet eine gegebene Handelsentscheidung und versucht, einen Trade auszuführen.
     * Die Implementierung entscheidet, ob und wie der Trade basierend auf dem Signal
     * und dem aktuellen Zustand des Marktes ausgeführt wird.
     *
     * @param decision Die aktuelle {@link SignalDecision} (Kauf-, Verkaufs- oder Halteentscheidung).
     * @param candle   Die aktuelle {@link Candle}, auf deren Basis die Entscheidung getroffen wurde.
     */
    void execute(SignalDecision decision, Candle candle);

    /**
     * Ruft eine Liste aller bisher von dieser Engine ausgeführten Trades ab.
     *
     * @return Eine {@link List} von {@link Trade}-Objekten, die die Historie der Trades darstellt.
     */
    List<Trade> getTrades();

    /**
     * Setzt den internen Zustand der Engine zurück, einschließlich aller ausgeführten Trades
     * und des Kapitals. Dies ist nützlich, um eine neue Simulations- oder Handelsperiode zu starten.
     */
    void reset();

    /**
     * Aktiviert den automatisierten Handel der Engine.
     * Nach dem Aufruf dieser Methode wird die Engine beginnen, Signale zu verarbeiten und Trades auszuführen.
     */
    void start();

    /**
     * Deaktiviert den automatisierten Handel der Engine.
     * Nach dem Aufruf dieser Methode wird die Engine keine weiteren Trades ausführen.
     * Offene Positionen bleiben typischerweise bestehen, werden aber nicht automatisch geschlossen.
     */
    void stop();

    /**
     * Überprüft, ob der automatisierte Handel der Engine aktuell aktiv ist.
     *
     * @return `true`, wenn der Handel aktiv ist; `false` sonst.
     */
    boolean isRunning();
}
