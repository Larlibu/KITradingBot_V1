package com.kitradingbot.model;

import java.util.List;

/**
 * Repräsentiert die umfassenden Ergebnisse eines Backtest-Durchlaufs.
 * Dieses Record-Objekt enthält sowohl statistische Performance-Metriken als auch
 * die detaillierten Daten (historische Kerzen und ausgeführte Trades) zur Visualisierung.
 *
 * @param strategyReturnPct Die prozentuale Rendite, die durch die angewandte Handelsstrategie erzielt wurde.
 * @param buyHoldReturnPct  Die prozentuale Rendite, die erzielt worden wäre, wenn man das Asset einfach gekauft und gehalten hätte (als Vergleichsbasis).
 * @param totalPnl          Der absolute Gesamtgewinn oder -verlust (Profit and Loss) in der Währung des Startkapitals.
 * @param totalTrades       Die Gesamtanzahl der während des Backtests ausgeführten Handelsaktionen (Käufe und Verkäufe).
 * @param winningTrades     Die Anzahl der Trades, die mit einem positiven Gewinn abgeschlossen wurden.
 * @param losingTrades      Die Anzahl der Trades, die mit einem Verlust abgeschlossen wurden.
 * @param winRate           Der Prozentsatz der gewinnbringenden Trades im Verhältnis zur Gesamtzahl der Trades.
 * @param history           Eine {@link List} von {@link Candle}-Objekten, die die vollständigen historischen Marktdaten des Backtests darstellen und für die Chart-Visualisierung verwendet werden können.
 * @param trades            Eine {@link List} von {@link Trade}-Objekten, die jeden einzelnen während des Backtests simulierten Handelsvorgang detailliert dokumentiert.
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
        List<Trade> trades
) {
}
