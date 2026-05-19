package com.kitradingbot.model;

import java.time.LocalDateTime;

/**
 * Repräsentiert eine einzelne, ausgeführte Handelsaktion (Trade) im System.
 * Dieses Record-Objekt dient der Dokumentation und Analyse von simulierten oder realen Trades.
 *
 * @param id                Eine eindeutige Identifikationsnummer für diesen Trade.
 * @param timestamp         Der genaue Zeitpunkt, zu dem der Trade ausgeführt wurde.
 * @param side              Die Art des Trades, entweder {@link SignalType#BUY} oder {@link SignalType#SELL}.
 * @param price             Der Ausführungspreis pro Einheit des Assets.
 * @param quantity          Die gehandelte Menge des Assets.
 * @param pnl               Der realisierte Gewinn oder Verlust (Profit and Loss) dieses Trades. Dieser Wert ist typischerweise nur bei SELL-Trades relevant.
 * @param capitalAfterTrade Das gesamte verfügbare Kapital nach Abschluss dieses Trades.
 */
public record Trade(
        long id,
        LocalDateTime timestamp,
        SignalType side,
        double price,
        double quantity,
        double pnl,
        double capitalAfterTrade
) {
}
