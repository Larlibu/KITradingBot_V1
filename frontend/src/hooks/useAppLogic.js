import { useMemo, useState, useEffect } from "react";
import { checkServerStatus } from "../api/tradingApi";
import { useTrading } from "./useTrading";
import { useIndicators } from "./useIndicators";
import { useBacktesting } from "./useBacktesting";
import { useReplay } from "./useReplay";
import { INDICATOR_TEMPLATES } from "../indicators/tradingViewIndicators";

/**
 * Controller-Hook fuer die App:
 * Nutzt ausschliesslich Backend-Daten (ta4j Source of Truth).
 */
export function useAppLogic() {
  const [viewMode, setViewMode] = useState("ANALYSE");
  const [serverStatus, setServerStatus] = useState({ backend: false, mlService: false });
  const [selectedIndicatorType, setSelectedIndicatorType] = useState(INDICATOR_TEMPLATES[0].type);
  const isLive = viewMode === "LIVE";

  const {
    indicators,
    useMlVote,
    setUseMlVote,
    addIndicator,
    removeIndicator,
    changeIndicatorParam
  } = useIndicators();

  const {
    backtest,
    backtestTrades,
    loadingBacktest,
    handleBacktest,
    evaluateDecisionAtIndex
  } = useBacktesting(indicators, useMlVote);

  const {
    marketData,
    signalSnapshot,
    liveTrades,
    handleStartTrading,
    handleStopTrading
  } = useTrading(indicators, useMlVote, isLive, false);

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

  useEffect(() => {
    const updateStatus = async () => {
      const status = await checkServerStatus();
      setServerStatus(status);
    };
    updateStatus();
    const interval = setInterval(updateStatus, 10000);
    return () => clearInterval(interval);
  }, []);

  const isReplayMode = replayEnabled && !isLive;

  const currentPrice = isReplayMode
    ? (replaySignal?.close ?? marketData[replayIndex]?.close ?? 0)
    : (signalSnapshot?.currentPrice ?? 0);

  const currentSignal = isReplayMode
    ? (replaySignal?.decision?.signal ?? "HOLD")
    : (signalSnapshot?.decision?.signal ?? "HOLD");

  const currentConfidence = isReplayMode
    ? (replaySignal?.prediction?.confidence ?? 0)
    : (signalSnapshot?.prediction?.confidence ?? 0);

  const currentTimestamp = isReplayMode
    ? (replaySignal?.timestamp ?? marketData[replayIndex]?.timestamp)
    : (signalSnapshot?.timestamp ?? marketData[marketData.length - 1]?.timestamp);

  const activeIndicatorValues = useMemo(() => {
    if (isReplayMode) {
      return (replaySignal?.votes || []).map(v => ({
        name: v.name,
        value: v.valueText
      }));
    }
    return (signalSnapshot?.indicatorValues || []).map((iv) => ({
      name: iv.name,
      value: iv.value.toFixed(2)
    }));
  }, [isReplayMode, replaySignal, signalSnapshot]);

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
