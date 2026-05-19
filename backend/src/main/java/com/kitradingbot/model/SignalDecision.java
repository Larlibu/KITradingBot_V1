package com.kitradingbot.model;

import java.time.LocalDateTime;

/**
 * Repräsentiert eine Handelsentscheidung, die von der {@link com.kitradingbot.signal.SignalEngine} getroffen wurde.
 * Dieses Record-Objekt fasst das Ergebnis der Strategieanalyse zusammen.
 *
 * @param signal     Der Typ des Signals ({@link SignalType#BUY}, {@link SignalType#SELL}, {@link SignalType#HOLD}).
 * @param reason     Eine textuelle Begründung für die getroffene Entscheidung (z.B. "RSI überverkauft", "Kein Konsens").
 * @param confidence Ein Vertrauenswert (zwischen 0.0 und 1.0) in die Entscheidung, oft abgeleitet von der KI-Konfidenz.
 * @param timestamp  Der genaue Zeitpunkt, zu dem diese Entscheidung getroffen wurde.
 */
public record SignalDecision(
        SignalType signal,
        String reason,
        double confidence,
        LocalDateTime timestamp
) {
}
