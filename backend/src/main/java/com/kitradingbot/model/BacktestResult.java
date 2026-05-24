package com.kitradingbot.model;

import java.util.List;

/**
 * Repraesentiert die umfassenden Ergebnisse eines Backtest-Durchlaufs.
 * Enthalt Performance-Metriken, historische Kerzen, Trades sowie detaillierte Signal-Entscheidungen.
 *
 * @param strategyReturnPct Die prozentuale Rendite der Strategie.
 * @param buyHoldReturnPct  Vergleichswerte: Rendite bei einfachem Halten.
 * @param totalPnl          Absoluter Gewinn/Verlust.
 * @param totalTrades       Anzahl der Trades.
 * @param winningTrades     Anzahl der Gewinn-Trades.
 * @param losingTrades      Anzahl der Verlust-Trades.
 * @param winRate           Prozentuale Gewinnrate.
 * @param history           Historische Marktdaten (Kerzen).
 * @param trades            Detaillierte Liste der simulierten Trades.
 * @param decisions         Detaillierte Signal-Entscheidungen fuer jeden Zeitpunkt der Historie.
 * @param indicatorHistory  Berechnete Indikatorwerte fuer jeden Zeitpunkt der Historie.
 */
public record BacktestResult(
        double strategyReturnPct,
        double buyHoldReturnPct,
        double totalPnl,
        int totalTrades,
        int winningTrades,
        int losingTrades,
        double winRate,
        List<Candle> history,
        List<Trade> trades,
        List<SignalDecision> decisions,
        List<List<IndicatorValue>> indicatorHistory
) {
}
