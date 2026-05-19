import { useMemo, useState, useEffect } from "react";
import { checkServerStatus } from "../api/tradingApi";
import { useTrading } from "./useTrading";
import { useIndicators } from "./useIndicators";
import { useBacktesting } from "./useBacktesting";
import { useReplay } from "./useReplay";
import { INDICATOR_TEMPLATES } from "../indicators/tradingViewIndicators";

/**
 * Controller-Hook für die App:
 * kapselt Zustand, Datenfluss und Aktionen unabhängig von der Darstellung.
 */
export function useAppLogic() {
  // --- Basiszustand der Seite ---
  const [viewMode, setViewMode] = useState("ANALYSE");
  const [serverStatus, setServerStatus] = useState({ backend: false, mlService: false });
  const [selectedIndicatorType, setSelectedIndicatorType] = useState(INDICATOR_TEMPLATES[0].type);
  const isLive = viewMode === "LIVE";

  // --- Strategie-/Indikatorzustand ---
  const {
    indicators,
    useMlVote,
    setUseMlVote,
    addIndicator,
    removeIndicator,
    changeIndicatorParam
  } = useIndicators();

  // --- Analyse-/Backtest-Controller ---
  const {
    backtest,
    backtestTrades,
    loadingBacktest,
    handleBacktest,
    evaluateDecisionAtIndex
  } = useBacktesting(indicators, useMlVote);

  // --- Live-Trading-Controller ---
  const {
    marketData,
    signalSnapshot,
    liveConsensus,
    liveTrades,
    handleStartTrading,
    handleStopTrading
  } = useTrading(indicators, useMlVote, isLive, false);

  // --- Replay-Controller ---
  const {
    replayEnabled,
    setReplayEnabled,
    replayIndex,
    replaySignal,
    replayMarkers,
    handleReplayStep,
    maxReplayIndex,
    isPlaying,
    togglePlay,
    playbackSpeed,
    setPlaybackSpeed,
    lastTradeSignal,
    resetTradeSignal
  } = useReplay(marketData, evaluateDecisionAtIndex);

  // --- Polling für Backend-/ML-Status ---
  useEffect(() => {
    const updateStatus = async () => {
      const status = await checkServerStatus();
      setServerStatus(status);
    };
    updateStatus();
    const interval = setInterval(updateStatus, 10000);
    return () => clearInterval(interval);
  }, []);

  // Replay ist nur im Analyse-Modus relevant.
  const isReplayMode = replayEnabled && !isLive;

  // Abgeleiteter Preis für Dashboard-Karten.
  const currentPrice = isReplayMode
    ? (replaySignal?.close ?? marketData[replayIndex]?.close ?? 0)
    : (liveConsensus?.close ?? signalSnapshot?.currentPrice ?? 0);

  // Abgeleitetes Signal für Dashboard-Karten.
  const currentSignal = isReplayMode
    ? (replaySignal?.decision?.signal ?? "HOLD")
    : (liveConsensus?.decision?.signal ?? signalSnapshot?.decision?.signal ?? "HOLD");

  // Abgeleitete KI-Konfidenz für Dashboard-Karten.
  const currentConfidence = isReplayMode
    ? (replaySignal?.prediction?.confidence ?? 0)
    : (liveConsensus?.prediction?.confidence ?? signalSnapshot?.prediction?.confidence ?? 0);

  // Abgeleiteter Zeitstempel für Dashboard-Karten.
  const currentTimestamp = isReplayMode
    ? (replaySignal?.timestamp ?? marketData[replayIndex]?.timestamp)
    : (signalSnapshot?.timestamp ?? marketData[marketData.length - 1]?.timestamp);

  // Normalisiert darzustellende Indikatorwerte je nach Modus (Replay vs. Live-Snapshot).
  const activeIndicatorValues = useMemo(() => {
    if (isReplayMode) {
      return indicators.map((configuredInd) => {
        const vote = (replaySignal?.votes || []).find((v) => v.id === configuredInd.id);
        return {
          name: configuredInd.label,
          value: vote?.valueText?.match(/[\d.-]+/)?.[0] || "..."
        };
      });
    }
    return (signalSnapshot?.indicatorValues || []).map((iv) => ({
      name: iv.name,
      value: iv.value.toFixed(2)
    }));
  }, [isReplayMode, replaySignal, signalSnapshot, indicators]);

  // Einheitliches ViewModel für die App-View.
  return {
    viewMode,
    setViewMode,
    serverStatus,
    selectedIndicatorType,
    setSelectedIndicatorType,
    isLive,
    indicators,
    useMlVote,
    setUseMlVote,
    addIndicator,
    removeIndicator,
    changeIndicatorParam,
    backtest,
    backtestTrades,
    loadingBacktest,
    handleBacktest,
    marketData,
    signalSnapshot,
    liveTrades,
    handleStartTrading,
    handleStopTrading,
    replayEnabled,
    setReplayEnabled,
    replayIndex,
    replaySignal,
    replayMarkers,
    handleReplayStep,
    maxReplayIndex,
    isPlaying,
    togglePlay,
    playbackSpeed,
    setPlaybackSpeed,
    lastTradeSignal,
    resetTradeSignal,
    currentPrice,
    currentSignal,
    currentConfidence,
    currentTimestamp,
    activeIndicatorValues,
    indicatorTemplates: INDICATOR_TEMPLATES
  };
}
