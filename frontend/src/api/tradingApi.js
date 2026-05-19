import api from "./client";
import axios from "axios";

/**
 * Ruft historische Marktdaten (Candles) vom Backend ab.
 * @param {number} [limit=200] - Die maximale Anzahl der abzurufenden Kerzen.
 * @param {string} [symbol="BTCUSDT"] - Das Handelssymbol.
 * @returns {Promise<Array<object>>} Eine Liste von Candle-Objekten.
 */
export async function fetchMarket(limit = 200, symbol = "BTCUSDT") {
  const response = await api.get("/market", { params: { limit, symbol } });
  return response.data;
}

/**
 * Ruft das aktuelle Handelssignal vom Backend ab.
 * Verwendet POST, um die Indikatorkonfiguration im Request Body zu senden.
 * @param {number} [candles=200] - Die Anzahl der Kerzen für die Analyse.
 * @param {string} [symbol="BTCUSDT"] - Das Handelssymbol.
 * @param {boolean} [useMl=false] - Gibt an, ob die KI für den Konsens genutzt werden soll.
 * @param {Array<object>} [indicators=[]] - Eine Liste der konfigurierten Indikatoren.
 * @returns {Promise<object>} Ein SignalSnapshot-Objekt.
 */
export async function fetchSignal(candles = 200, symbol = "BTCUSDT", useMl = false, indicators = []) {
  const response = await api.post("/signal", indicators, {
    params: { candles, symbol, useMl }
  });
  return response.data;
}

/**
 * Ruft eine Liste aller ausgeführten Trades vom Backend ab.
 * @returns {Promise<Array<object>>} Eine Liste von Trade-Objekten.
 */
export async function fetchTrades() {
  const response = await api.get("/trades");
  return response.data;
}

/**
 * Startet den automatisierten Handel im Backend.
 * @returns {Promise<object>} Eine Statusmeldung.
 */
export async function startTrading() {
  const response = await api.post("/trades/start");
  return response.data;
}

/**
 * Stoppt den automatisierten Handel im Backend.
 * @returns {Promise<object>} Eine Statusmeldung.
 */
export async function stopTrading() {
  const response = await api.post("/trades/stop");
  return response.data;
}

/**
 * Führt einen Backtest im Backend durch.
 * Verwendet POST, um die Indikatorkonfiguration im Request Body zu senden.
 * @param {number} [candles=500] - Die Anzahl der Kerzen für den Backtest.
 * @param {string} [symbol="BTCUSDT"] - Das Handelssymbol.
 * @param {boolean} [useMl=false] - Gibt an, ob die KI für den Konsens genutzt werden soll.
 * @param {Array<object>} [indicators=[]] - Eine Liste der konfigurierten Indikatoren.
 * @returns {Promise<object>} Ein BacktestResult-Objekt.
 */
export async function runBacktest(candles = 500, symbol = "BTCUSDT", useMl = false, indicators = []) {
  const response = await api.post("/backtest", indicators, {
    params: { candles, symbol, useMl }
  });
  return response.data;
}

/**
 * Fordert eine einzelne Vorhersage vom ML-Service über das Backend an.
 * @param {Array<number>} features - Ein Array von Features für die Vorhersage.
 * @returns {Promise<object>} Ein MlPredictionResponse-Objekt.
 */
export async function requestPrediction(features) {
  const response = await api.post("/predict", { features });
  return response.data;
}

/**
 * Löst das Training des ML-Modells im Backend aus.
 * @returns {Promise<object>} Eine Statusmeldung.
 */
export async function triggerMlTraining() {
  const response = await api.post("/predict/train");
  return response.data;
}

/**
 * Löst den Download und die Vorbereitung neuer Marktdaten im Backend aus.
 * @returns {Promise<object>} Eine Statusmeldung.
 */
export async function triggerDataUpdate() {
  const response = await api.post("/predict/data/update");
  return response.data;
}

/**
 * Ruft den Status des Daten-Updates vom Backend ab.
 * @returns {Promise<object>} Eine Map mit dem Status des Daten-Updates.
 */
export async function fetchUpdateStatus() {
  const response = await api.get("/predict/data/status");
  return response.data;
}

/**
 * Überprüft den Status der Backend-Dienste (Java und Python ML-Service).
 * @returns {Promise<object>} Ein Objekt mit den Status von Backend und mlService.
 */
export async function checkServerStatus() {
  const results = { backend: false, mlService: false };
  try {
    // Prüft das Java-Backend
    const backendRes = await api.get("/market?limit=1", { timeout: 3000 });
    results.backend = Array.isArray(backendRes.data);
  } catch (e) { results.backend = false; }

  try {
    // Prüft den Python ML-Service direkt (da er auf einem anderen Port läuft)
    const mlRes = await axios.get("http://localhost:8000/health", { timeout: 2000 });
    results.mlService = mlRes.data?.status === "ok";
  } catch (e) { results.mlService = false; }
  return results;
}
