import { useState, useCallback, useEffect } from "react";
import { fetchMarket, fetchSignal, fetchTrades, startTrading, stopTrading, requestPrediction } from "../api/tradingApi";
import { usePolling } from "./usePolling";
import { evaluateIndicatorVotes, buildConsensus, buildMlFeatures, mlVoteFromPrediction, ML_CONFIG } from "../indicators/tradingViewIndicators";

/**
 * Custom Hook zur Verwaltung der Live-Trading-Daten und -Logik im Frontend.
 * Ruft regelmäßig Marktdaten, Handelssignale und Trade-Historie vom Backend ab.
 *
 * @param {Array<object>} indicators - Die Liste der aktuell konfigurierten Indikatoren.
 * @param {boolean} useMlVote - Gibt an, ob die KI-Vorhersage in die Strategie einbezogen werden soll.
 * @param {boolean} isLiveMode - Gibt an, ob sich die Anwendung im Live-Modus befindet.
 * @param {boolean} replayEnabled - Gibt an, ob der Replay-Modus aktiv ist (um Polling zu pausieren).
 * @returns {object} Ein Objekt mit Live-Daten, Signal-Snapshots, Ladezustand und Funktionen.
 */
export function useTrading(indicators, useMlVote, isLiveMode, replayEnabled) {
  /** @type {[Array<object>, Function]} Die aktuellen Marktdaten (Candles). */
  const [marketData, setMarketData] = useState([]);
  /** @type {[object|null, Function]} Der letzte Signal-Snapshot vom Backend. */
  const [signalSnapshot, setSignalSnapshot] = useState(null);
  /** @type {[object|null, Function]} Der im Frontend berechnete Konsens für die neueste Kerze. */
  const [liveConsensus, setLiveConsensus] = useState(null);
  /** @type {[Array<object>, Function]} Die Liste der Live-Trades. */
  const [liveTrades, setLiveTrades] = useState([]);

  /**
   * Evaluiert die Handelsstrategie für die neueste Kerze im Frontend.
   * Diese Funktion wird für die Anzeige im Dashboard verwendet, die eigentliche
   * Signalentscheidung im Live-Modus trifft das Backend.
   *
   * @param {Array<object>} candles - Die Liste der Candlestick-Daten.
   * @returns {Promise<object>} Ein Objekt mit dem Status, der Entscheidung, Prediction, Votes und Kerzendaten.
   */
  const evaluateDecisionForLatestCandle = useCallback(async (candles) => {
    if (!candles.length) return { status: "WARMUP" };

    const latestIndex = candles.length - 1;
    const indicatorEvaluation = evaluateIndicatorVotes(candles, latestIndex, indicators);
    const mlWarmupIndex = Math.max(ML_CONFIG.smaPeriod - 1, ML_CONFIG.rsiPeriod);
    let minReadyIndex = Math.max(indicatorEvaluation.minReadyIndex ?? 0, useMlVote ? mlWarmupIndex : 0);

    if (latestIndex < minReadyIndex || !indicatorEvaluation.ready) {
      return { status: "WARMUP", decision: { signal: "HOLD", reason: "Aufwärmphase..." } };
    }

    let prediction = null;
    let mlVote = "HOLD";
    // KI-Vorhersage nur anfragen, wenn useMlVote aktiviert ist.
    if (useMlVote) {
      const mlFeatures = buildMlFeatures(candles, latestIndex);
      if (!mlFeatures) return { status: "WARMUP" };
      try {
        prediction = await requestPrediction(mlFeatures.features);
        mlVote = mlVoteFromPrediction(prediction, ML_CONFIG.confidenceThreshold);
      } catch (e) {
        console.error("KI-Anfrage im Live-Modus fehlgeschlagen:", e);
      }
    }

    const votes = [...indicatorEvaluation.votes];
    if (useMlVote && prediction) {
      votes.unshift({ id: "ml-vote", name: "KI", signal: mlVote, valueText: `${prediction.label} (${(prediction.confidence * 100).toFixed(0)}%)` });
    }

    // Die Konsens-Logik im Frontend ist nur für die Anzeige im Dashboard relevant.
    // Im Live-Modus wird die tatsächliche Entscheidung vom Backend getroffen, das das useMlVote Flag erhält.
    const consensus = buildConsensus(votes, useMlVote);
    return {
      status: "READY",
      decision: { signal: consensus.signal, reason: consensus.reason },
      prediction,
      votes,
      close: candles[latestIndex].close
    };
  }, [indicators, useMlVote]); // Abhängigkeiten: indicators und useMlVote

  /**
   * Aktualisiert alle Live-Trading-Daten (Markt, Signal, Trades).
   */
  const refreshTradingData = useCallback(async () => {
    try {
      // Ruft Marktdaten, Signal und Trades parallel ab.
      // Das useMlVote Flag und die Indikatoren werden an fetchSignal übergeben.
      const [market, signal, currentTrades] = await Promise.all([
        fetchMarket(200),
        fetchSignal(200, "BTCUSDT", useMlVote, indicators),
        fetchTrades()
      ]);
      setMarketData(market);
      setSignalSnapshot(signal);
      setLiveTrades(currentTrades);
      if (market.length > 0) {
        // Evaluiert den Konsens für die neueste Kerze im Frontend für die Dashboard-Anzeige.
        const latest = await evaluateDecisionForLatestCandle(market);
        setLiveConsensus(latest);
      }
    } catch (error) {
      console.warn("Fehler beim Aktualisieren der Live-Daten:", error.message);
    }
  }, [evaluateDecisionForLatestCandle, useMlVote, indicators]); // Abhängigkeiten: evaluateDecisionForLatestCandle, useMlVote, indicators

  /**
   * Effekt zum initialen Laden der Daten beim Komponenten-Mount.
   */
  useEffect(() => {
    refreshTradingData();
  }, [refreshTradingData]); // Abhängigkeit: refreshTradingData

  /**
   * Polling-Mechanismus für regelmäßige Datenaktualisierungen im Live-Modus.
   * Das Polling wird pausiert, wenn der Replay-Modus aktiv ist.
   */
  usePolling(() => refreshTradingData(), 5000, isLiveMode && !replayEnabled);

  return {
    marketData,
    signalSnapshot,
    liveConsensus,
    liveTrades,
    // Funktionen zum Starten/Stoppen des Handels, die nach Ausführung die Daten aktualisieren.
    handleStartTrading: () => startTrading().then(refreshTradingData),
    handleStopTrading: () => stopTrading().then(refreshTradingData),
  };
}
