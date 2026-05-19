package com.kitradingbot.backtesting;

import com.kitradingbot.model.BacktestResult;
import com.kitradingbot.model.IndicatorConfig;
import java.util.List;

/**
 * Interface für die Simulation von Handelsstrategien auf historischen Daten.
 * Eine BacktestEngine ist dafür verantwortlich, eine gegebene Handelsstrategie
 * auf einer historischen Reihe von Marktdaten auszuführen und die Performance
 * der Strategie zu bewerten.
 */
public interface BacktestEngine {
    /**
     * Führt einen Backtest für eine Handelsstrategie auf historischen Daten durch.
     * Die Strategie wird basierend auf den übergebenen Indikatorkonfigurationen
     * und dem optionalen Einbezug der KI-Vorhersage simuliert.
     *
     * @param symbol     Das Handelssymbol, für das der Backtest durchgeführt werden soll (z.B. "BTCUSDT").
     * @param candles    Die Anzahl der historischen Kerzen, die für die Simulation verwendet werden sollen.
     * @param useMl      Ein `boolean`-Flag, das angibt, ob die KI-Vorhersage in die Signalgenerierung einbezogen werden soll.
     * @param indicators Eine {@link List} von {@link IndicatorConfig}-Objekten, die die Konfiguration der
     *                   vom Benutzer ausgewählten Indikatoren definieren.
     * @return Ein {@link BacktestResult}-Objekt, das die Performance-Metriken,
     *         die historischen Kerzen und die simulierten Trades des Backtests enthält.
     */
    BacktestResult run(String symbol, int candles, boolean useMl, List<IndicatorConfig> indicators);
}
