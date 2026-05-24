import { useState, useCallback, useEffect } from "react";
import { fetchMarket, fetchSignal, fetchTrades, startTrading, stopTrading } from "../api/tradingApi";
import { usePolling } from "./usePolling";

/**
 * Custom Hook zur Verwaltung der Live-Trading-Daten.
 * Alle Berechnungen erfolgen im Backend (ta4j), das Frontend zeigt nur die Snapshots an.
 */
export function useTrading(indicators, useMlVote, isLiveMode, replayEnabled) {
  const [marketData, setMarketData] = useState([]);
  const [signalSnapshot, setSignalSnapshot] = useState(null);
  const [liveTrades, setLiveTrades] = useState([]);

  const refreshTradingData = useCallback(async () => {
    try {
      const [market, signal, currentTrades] = await Promise.all([
        fetchMarket(200),
        fetchSignal(200, "BTCUSDT", useMlVote, indicators),
        fetchTrades()
      ]);
      setMarketData(market);
      setSignalSnapshot(signal);
      setLiveTrades(currentTrades);
    } catch (error) {
      console.warn("Fehler beim Aktualisieren der Live-Daten:", error.message);
    }
  }, [useMlVote, indicators]);

  useEffect(() => {
    refreshTradingData();
  }, [refreshTradingData]);

  usePolling(() => refreshTradingData(), 5000, isLiveMode && !replayEnabled);

  return {
    marketData,
    signalSnapshot,
    liveTrades,
    handleStartTrading: () => startTrading().then(refreshTradingData),
    handleStopTrading: () => stopTrading().then(refreshTradingData),
  };
}
