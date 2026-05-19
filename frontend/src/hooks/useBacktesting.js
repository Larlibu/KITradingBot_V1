import { useState, useCallback } from "react";
import { runBacktest, requestPrediction } from "../api/tradingApi";
import { evaluateIndicatorVotes, buildMlFeatures, mlVoteFromPrediction, ML_CONFIG } from "../indicators/tradingViewIndicators";

/**
 * Custom Hook zur Verwaltung der Backtesting-Logik im Frontend.
 * Bietet Funktionen zum Ausführen eines schnellen Backtests im Backend
 * und zur schrittweisen Evaluierung der Strategie für den Replay-Modus.
 *
 * @param {Array<object>} indicators - Die Liste der aktuell konfigurierten Indikatoren.
 * @param {boolean} useMlVote - Gibt an, ob die KI-Vorhersage in die Strategie einbezogen werden soll.
 * @returns {object} Ein Objekt mit Backtest-Ergebnissen, Ladezustand und Funktionen.
 */
export function useBacktesting(indicators, useMlVote) {
  /** @type {[object|null, Function]} Der Ergebnis-Snapshot des letzten Backend-Backtests. */
  const [backtest, setBacktest] = useState(null);
  /** @type {[Array<object>, Function]} Die Liste der Trades aus dem letzten Backend-Backtest. */
  const [backtestTrades, setBacktestTrades] = useState([]);
  /** @type {[boolean, Function]} Ladezustand für den Backend-Backtest. */
  const [loadingBacktest, setLoadingBacktest] = useState(false);

  /**
   * Führt einen schnellen Backtest im Backend aus.
   * Sendet die aktuelle Indikatorkonfiguration und das KI-Flag an den Java-Service.
   *
   * @param {string} [symbol="BTCUSDT"] - Das Handelssymbol für den Backtest.
   * @param {number} [candlesCount=500] - Die Anzahl der Kerzen für den Backtest.
   * @returns {Promise<object|void>} Das BacktestResult-Objekt vom Backend oder void bei Fehler.
   */
  const handleBacktest = useCallback(async (symbol = "BTCUSDT", candlesCount = 500) => {
    setLoadingBacktest(true);
    try {
      // Nutzt die Backend-Engine (Java), die wir erweitert haben,
      // und übergibt den aktuellen useMlVote Zustand sowie die Indikatoren.
      const result = await runBacktest(candlesCount, symbol, useMlVote, indicators);

      setBacktest(result);
      setBacktestTrades(result.trades || []);

      return result;
    } catch (err) {
      console.error("Backend Backtest Error:", err);
      alert("Fehler beim Backend-Backtest. Prüfe die Konsole für Details.");
    } finally {
      setLoadingBacktest(false);
    }
  }, [useMlVote, indicators]); // Abhängigkeiten: useMlVote und indicators

  /**
   * Evaluiert die Handelsstrategie für eine einzelne Kerze an einem bestimmten Index.
   * Diese Funktion wird hauptsächlich für den Frontend-Replay-Modus verwendet.
   * Sie berechnet Indikator-Votes und holt optional eine KI-Vorhersage ein.
   *
   * @param {Array<object>} candles - Die Liste der Candlestick-Daten.
   * @param {number} index - Der Index der Kerze, die evaluiert werden soll.
   * @returns {Promise<object>} Ein Objekt mit dem Status, der Entscheidung, Prediction, Votes und Kerzendaten.
   */
  const evaluateDecisionAtIndex = useCallback(async (candles, index) => {
    // Prüft auf gültige Eingabedaten.
    if (!candles.length || index < 0 || index >= candles.length) return { status: "WARMUP" };

    // 1. Indikatoren im Frontend evaluieren.
    const indicatorEvaluation = evaluateIndicatorVotes(candles, index, indicators);

    // 2. Warmup-Periode für ML-Features bestimmen.
    const mlWarmupIndex = Math.max(ML_CONFIG.smaPeriod - 1, ML_CONFIG.rsiPeriod);
    // Der minimale Index, ab dem eine vollständige Strategie-Evaluierung möglich ist.
    let minReadyIndex = Math.max(indicatorEvaluation.minReadyIndex ?? 0, useMlVote ? mlWarmupIndex : 0);

    // Wenn sich die aktuelle Kerze noch in der Warmup-Periode befindet, ist keine Entscheidung möglich.
    if (index < minReadyIndex || !indicatorEvaluation.ready) {
      return {
        status: "WARMUP",
        minReadyIndex,
        votes: indicatorEvaluation.votes // Indikator-Votes auch im Warmup mitschicken (mit "HOLD")
      };
    }

    let prediction = null;
    let mlVote = "HOLD";
    // 3. KI-Vorhersage einholen, falls useMlVote aktiviert ist.
    if (useMlVote) {
      const mlFeatures = buildMlFeatures(candles, index);
      // Wenn ML-Features nicht berechnet werden können (z.B. wegen Warmup), zurückkehren.
      if (!mlFeatures) return { status: "WARMUP", minReadyIndex, votes: indicatorEvaluation.votes };
      try {
        prediction = await requestPrediction(mlFeatures.features);
        mlVote = mlVoteFromPrediction(prediction, ML_CONFIG.confidenceThreshold);
      } catch (e) {
        console.error("KI-Anfrage im Replay fehlgeschlagen:", e);
        // Bei Fehler der KI-Anfrage wird der Zustand als Warmup behandelt.
        return { status: "WARMUP", minReadyIndex, votes: indicatorEvaluation.votes };
      }
    }

    // 4. Konsensbildung für die Frontend-Visualisierung.
    // Die `votes`-Liste enthält die Ergebnisse der einzelnen Indikatoren.
    const votes = [...indicatorEvaluation.votes];
    if (useMlVote && prediction) {
      // Fügt den KI-Vote am Anfang der Liste hinzu, wenn KI aktiv ist.
      votes.unshift({
        id: "ml-vote",
        name: "KI",
        signal: mlVote,
        valueText: `${prediction.label} (${(prediction.confidence * 100).toFixed(0)}%)`
      });
    }

    // Bestimmt, ob alle technischen Indikatoren ein Kauf- oder Verkaufssignal geben.
    const techBuy = indicatorEvaluation.votes.every(v => v.signal === "BUY");
    const techSell = indicatorEvaluation.votes.every(v => v.signal === "SELL");

    let finalSignal = "HOLD";
    // Die finale Signalentscheidung basiert auf dem Konsens der Indikatoren und optional der KI.
    if (useMlVote) {
      if (techBuy && mlVote === "BUY") finalSignal = "BUY";
      if (techSell && mlVote === "SELL") finalSignal = "SELL";
    } else {
      // Wenn KI nicht verwendet wird, basiert die Entscheidung nur auf den technischen Indikatoren.
      if (techBuy) finalSignal = "BUY";
      if (techSell) finalSignal = "SELL";
    }

    // Gibt das Ergebnis der Strategie-Evaluierung zurück.
    return {
      status: "READY",
      decision: {
        signal: finalSignal,
        reason: useMlVote ? `KI (${mlVote}) + Technik` : "Technik Indikatoren"
      },
      prediction,
      votes, // WICHTIG: Diese Daten werden für das Dashboard benötigt!
      close: candles[index].close,
      timestamp: candles[index].timestamp
    };
  }, [indicators, useMlVote]); // Abhängigkeiten: indicators und useMlVote

  return {
    backtest,
    backtestTrades,
    loadingBacktest,
    handleBacktest,
    evaluateDecisionAtIndex
  };
}
