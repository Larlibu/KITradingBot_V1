import { useState, useCallback } from "react";
import { runBacktest } from "../api/tradingApi";

/**
 * Custom Hook fuer das Backtesting.
 * Nutzt die ta4j-Engine vom Backend.
 */
export function useBacktesting(indicators, useMlVote) {
  const [backtest, setBacktest] = useState(null);
  const [backtestTrades, setBacktestTrades] = useState([]);
  const [loadingBacktest, setLoadingBacktest] = useState(false);

  const handleBacktest = useCallback(async (symbol = "BTCUSDT", candlesCount = 500) => {
    setLoadingBacktest(true);
    try {
      const result = await runBacktest(candlesCount, symbol, useMlVote, indicators);
      setBacktest(result);
      setBacktestTrades(result.trades || []);
      return result;
    } catch (err) {
      console.error("Backend Backtest Error:", err);
    } finally {
      setLoadingBacktest(false);
    }
  }, [useMlVote, indicators]);

  /**
   * Holt die vorberechnete Entscheidung fuer einen Index aus dem Backtest-Ergebnis.
   */
  const evaluateDecisionAtIndex = useCallback(async (candles, index) => {
    if (!candles.length || index < 0 || index >= candles.length) return { status: "WARMUP" };

    if (backtest && backtest.decisions && backtest.decisions[index]) {
        const backendDecision = backtest.decisions[index];
        const backendInds = backtest.indicatorHistory ? backtest.indicatorHistory[index] : [];

        const votes = backendInds.map(iv => ({
            id: iv.name,
            name: iv.name,
            signal: "INFO",
            valueText: iv.value.toFixed(2)
        }));

        if (backendDecision.reason === "Warmup") {
            return { status: "WARMUP", votes };
        }

        return {
            status: "READY",
            decision: {
                signal: backendDecision.signal,
                reason: "ta4j (Backend)"
            },
            votes,
            close: candles[index].close,
            timestamp: candles[index].timestamp
        };
    }

    return { status: "WARMUP", votes: [] };
  }, [backtest]);

  return {
    backtest,
    backtestTrades,
    loadingBacktest,
    handleBacktest,
    evaluateDecisionAtIndex
  };
}
