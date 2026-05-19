package com.kitradingbot.service;

/**
 * Eine benutzerdefinierte RuntimeException für Fehler, die spezifisch für die Geschäftslogik
 * des Trading-Bots sind.
 * Diese Exception wird verwendet, um kontrollierte Fehlerbedingungen zu signalisieren,
 * die vom {@link com.kitradingbot.config.GlobalExceptionHandler} abgefangen und
 * in eine standardisierte API-Fehlerantwort umgewandelt werden können.
 */
public class TradingBotException extends RuntimeException {
    /**
     * Konstruktor für TradingBotException.
     *
     * @param message Eine detaillierte Nachricht, die den Grund der Ausnahme beschreibt.
     */
    public TradingBotException(String message) {
        super(message);
    }
}
