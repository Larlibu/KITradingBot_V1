/**
 * @file Enthält Hilfsfunktionen und Konfigurationen für technische Indikatoren
 *       und die Strategie-Evaluierung im Frontend.
 *       Diese Funktionen spiegeln die Logik des Java-Backends wider,
 *       um Konsistenz zwischen Replay und Live-Handel zu gewährleisten.
 */

/** @type {number} Minimaler Wert für Indikatorparameter. */
const MIN_PARAM = -1_000_000;
/** @type {number} Maximaler Wert für Indikatorparameter. */
const MAX_PARAM = 1_000_000;

/**
 * Konfigurationseinstellungen für die Machine Learning Integration.
 * @type {Readonly<{confidenceThreshold: number, smaPeriod: number, rsiPeriod: number}>}
 */
export const ML_CONFIG = Object.freeze({
  confidenceThreshold: 0.6, // Mindestkonfidenz der KI, damit ihr Signal berücksichtigt wird.
  smaPeriod: 14, // SMA-Periode für die ML-Feature-Berechnung.
  rsiPeriod: 14  // RSI-Periode für die ML-Feature-Berechnung.
});

/**
 * Vorlagen für die verschiedenen unterstützten Indikatoren.
 * Definiert Typ, Label, Standardparameter und Felder für die UI-Konfiguration.
 * @type {Readonly<Array<object>>}
 */
export const INDICATOR_TEMPLATES = Object.freeze([
  {
    type: "SMA",
    label: "SMA",
    defaults: { length: 20 },
    fields: [{ key: "length", label: "Length", min: 2, max: 300, step: 1 }]
  },
  {
    type: "EMA",
    label: "EMA",
    defaults: { length: 21 },
    fields: [{ key: "length", label: "Length", min: 2, max: 300, step: 1 }]
  },
  {
    type: "RSI",
    label: "RSI",
    defaults: { length: 14, overbought: 70, oversold: 30 },
    fields: [
      { key: "length", label: "Length", min: 2, max: 200, step: 1 },
      { key: "overbought", label: "Overbought", min: 50, max: 95, step: 1 },
      { key: "oversold", label: "Oversold", min: 5, max: 50, step: 1 }
    ]
  },
  {
    type: "MACD",
    label: "MACD",
    defaults: { fast: 12, slow: 26, signal: 9 },
    fields: [
      { key: "fast", label: "Fast", min: 2, max: 100, step: 1 },
      { key: "slow", label: "Slow", min: 3, max: 200, step: 1 },
      { key: "signal", label: "Signal", min: 2, max: 100, step: 1 }
    ]
  },
  {
    type: "BBANDS",
    label: "Bollinger",
    defaults: { length: 20, mult: 2 },
    fields: [
      { key: "length", label: "Length", min: 2, max: 300, step: 1 },
      { key: "mult", label: "StdDev", min: 0.5, max: 5, step: 0.1 }
    ]
  }
]);

/**
 * Eine Map zur schnellen Suche von Indikatorvorlagen nach ihrem Typ.
 * @type {Map<string, object>}
 */
const templateByType = new Map(INDICATOR_TEMPLATES.map((item) => [item.type, item]));

/**
 * Begrenzt einen Wert auf einen bestimmten Bereich.
 * @param {number} value - Der zu begrenzende Wert.
 * @param {number} min - Der minimale erlaubte Wert.
 * @param {number} max - Der maximale erlaubte Wert.
 * @returns {number} Der begrenzte Wert.
 */
function clamp(value, min, max) {
  return Math.max(min, Math.min(max, value));
}

/**
 * Konvertiert einen Wert sicher in eine Zahl und begrenzt ihn.
 * @param {*} value - Der zu konvertierende Wert.
 * @param {number} fallback - Der Fallback-Wert, wenn die Konvertierung fehlschlägt.
 * @param {number} [min=MIN_PARAM] - Minimaler erlaubter Wert.
 * @param {number} [max=MAX_PARAM] - Maximaler erlaubter Wert.
 * @returns {number} Der konvertierte und begrenzte numerische Wert.
 */
function asNumber(value, fallback, min = MIN_PARAM, max = MAX_PARAM) {
  const n = Number(value);
  if (!Number.isFinite(n)) {
    return fallback;
  }
  return clamp(n, min, max);
}

/**
 * Berechnet den Durchschnitt einer Liste von Zahlen.
 * @param {Array<number>} values - Die Liste der Zahlen.
 * @returns {number} Der Durchschnitt.
 */
function avg(values) {
  return values.reduce((sum, val) => sum + val, 0) / values.length;
}

/**
 * Berechnet den Simple Moving Average (SMA) für eine Kerzenliste.
 * @param {Array<object>} candles - Die Candlestick-Daten.
 * @param {number} endIndex - Der Endindex für die Berechnung.
 * @param {number} length - Die Periode des SMA.
 * @returns {number|null} Der SMA-Wert oder null, wenn nicht genügend Daten vorhanden sind.
 */
function calcSma(candles, endIndex, length) {
  if (endIndex < length - 1) {
    return null;
  }
  let sum = 0;
  for (let i = endIndex - length + 1; i <= endIndex; i += 1) {
    sum += candles[i].close;
  }
  return sum / length;
}

/**
 * Berechnet den Exponential Moving Average (EMA) für eine Kerzenliste.
 * @param {Array<object>} candles - Die Candlestick-Daten.
 * @param {number} endIndex - Der Endindex für die Berechnung.
 * @param {number} length - Die Periode des EMA.
 * @returns {number|null} Der EMA-Wert oder null, wenn nicht genügend Daten vorhanden sind.
 */
function calcEma(candles, endIndex, length) {
  if (endIndex < length - 1) {
    return null;
  }
  // Initialer EMA-Wert (SMA der ersten 'length' Kerzen)
  let seed = 0;
  for (let i = 0; i < length; i += 1) {
    seed += candles[i].close;
  }
  let ema = seed / length;
  const k = 2 / (length + 1); // Glättungsfaktor
  // Berechnung des EMA für die restlichen Kerzen
  for (let i = length; i <= endIndex; i += 1) {
    ema = candles[i].close * k + ema * (1 - k);
  }
  return ema;
}

/**
 * Berechnet den Relative Strength Index (RSI) für eine Kerzenliste.
 * @param {Array<object>} candles - Die Candlestick-Daten.
 * @param {number} endIndex - Der Endindex für die Berechnung.
 * @param {number} length - Die Periode des RSI.
 * @returns {number|null} Der RSI-Wert oder null, wenn nicht genügend Daten vorhanden sind.
 */
function calcRsi(candles, endIndex, length) {
  if (endIndex < length) { // RSI benötigt 'length' + 1 Kerzen für die erste Berechnung
    return null;
  }
  let gains = 0;
  let losses = 0;
  // Berechnet Gewinne und Verluste über die Periode
  for (let i = endIndex - length + 1; i <= endIndex; i += 1) {
    const diff = candles[i].close - candles[i - 1].close;
    if (diff >= 0) {
      gains += diff;
    } else {
      losses += Math.abs(diff);
    }
  }
  if (losses === 0) {
    return 100; // Unendlich hoher RSI, wenn keine Verluste
  }
  const rs = gains / losses; // Relative Stärke
  return 100 - 100 / (1 + rs); // RSI-Formel
}

/**
 * Berechnet die Standardabweichung einer Kerzenreihe.
 * @param {Array<object>} candles - Die Candlestick-Daten.
 * @param {number} endIndex - Der Endindex für die Berechnung.
 * @param {number} length - Die Periode.
 * @param {number} mean - Der Durchschnitt (z.B. SMA) über die Periode.
 * @returns {number|null} Die Standardabweichung oder null, wenn nicht genügend Daten vorhanden sind.
 */
function calcStdDev(candles, endIndex, length, mean) {
  if (endIndex < length - 1) {
    return null;
  }
  let variance = 0;
  for (let i = endIndex - length + 1; i <= endIndex; i += 1) {
    const diff = candles[i].close - mean;
    variance += diff * diff;
  }
  return Math.sqrt(variance / length);
}

/**
 * Berechnet den EMA aus einer Liste von Werten (nicht Kerzen).
 * Wird intern für MACD-Berechnungen verwendet.
 * @param {Array<number>} values - Die Liste der numerischen Werte.
 * @param {number} length - Die Periode des EMA.
 * @returns {number|null} Der EMA-Wert oder null, wenn nicht genügend Daten vorhanden sind.
 */
function emaFromValues(values, length) {
  if (values.length < length) {
    return null;
  }
  let ema = avg(values.slice(0, length));
  const k = 2 / (length + 1);
  for (let i = length; i < values.length; i += 1) {
    ema = values[i] * k + ema * (1 - k);
  }
  return ema;
}

/**
 * Berechnet den Moving Average Convergence Divergence (MACD) Indikator.
 * @param {Array<object>} candles - Die Candlestick-Daten.
 * @param {number} endIndex - Der Endindex für die Berechnung.
 * @param {number} fastLength - Periode für den schnellen EMA.
 * @param {number} slowLength - Periode für den langsamen EMA.
 * @param {number} signalLength - Periode für die Signal-Linie.
 * @returns {object|null} Ein Objekt { macd, signal } oder null, wenn nicht genügend Daten vorhanden sind.
 */
function calcMacd(candles, endIndex, fastLength, slowLength, signalLength) {
  // MACD benötigt, dass slowLength > fastLength
  if (fastLength >= slowLength || endIndex < slowLength + signalLength - 2) {
    return null;
  }

  const macdValues = [];
  // Berechnet die MACD-Linie (Differenz zwischen schnellem und langsamem EMA)
  for (let i = slowLength - 1; i <= endIndex; i += 1) {
    const fast = calcEma(candles, i, fastLength);
    const slow = calcEma(candles, i, slowLength);
    if (fast == null || slow == null) {
      continue;
    }
    macdValues.push(fast - slow);
  }

  if (macdValues.length < signalLength) {
    return null;
  }

  const macd = macdValues[macdValues.length - 1]; // Aktueller MACD-Wert
  const signal = emaFromValues(macdValues, signalLength); // Signal-Linie (EMA des MACD)
  if (signal == null) {
    return null;
  }
  return { macd, signal };
}

/**
 * Berechnet die Bollinger Bänder (Upper, Lower, Middle) für eine Kerzenliste.
 * @param {Array<object>} candles - Die Candlestick-Daten.
 * @param {number} endIndex - Der Endindex für die Berechnung.
 * @param {number} period - Die Periode für den SMA (Middle Band) und die Standardabweichung.
 * @param {number} mult - Der Multiplikator für die Standardabweichung.
 * @returns {object|null} Ein Objekt { upper, lower, middle } oder null, wenn nicht genügend Daten vorhanden sind.
 */
function calculateBbands(candles, endIndex, period, mult) {
    if (endIndex < period - 1) {
        return null;
    }
    const window = candles.slice(endIndex - period + 1, endIndex + 1);
    const avg = window.reduce((sum, c) => sum + c.close, 0) / period; // Mittleres Band (SMA)

    const squareDiffs = window.map(c => Math.pow(c.close - avg, 2));
    const stdDev = Math.sqrt(squareDiffs.reduce((sum, d) => sum + d, 0) / period); // Standardabweichung

    const upper = avg + stdDev * mult;
    const lower = avg - stdDev * mult;

    return { upper, lower, middle: avg };
}

/**
 * Leitet ein Handelssignal ab, indem zwei Werte verglichen werden.
 * @param {number} a - Der erste Wert (z.B. Schlusskurs).
 * @param {number} b - Der zweite Wert (z.B. Indikatorwert).
 * @returns {string} "BUY", "SELL" oder "HOLD".
 */
function signalFromCompare(a, b) {
  if (a > b) {
    return "BUY";
  }
  if (a < b) {
    return "SELL";
  }
  return "HOLD";
}

/**
 * Bestimmt den minimalen Index (Warmup-Periode), ab dem ein Indikator berechnet werden kann.
 * @param {object} indicator - Das Indikator-Konfigurationsobjekt.
 * @returns {number} Der minimale Index.
 */
export function minIndexForIndicator(indicator) {
  if (!indicator) {
    return 0;
  }
  // Logik für jeden Indikatortyp, um die benötigte Mindestanzahl an Kerzen zu bestimmen.
  if (indicator.type === "SMA") {
    const length = Math.round(asNumber(indicator.params.length, 20, 2, 300));
    return Math.max(0, length - 1);
  }
  if (indicator.type === "EMA") {
    const length = Math.round(asNumber(indicator.params.length, 21, 2, 300));
    return Math.max(0, length - 1);
  }
  if (indicator.type === "RSI") {
    const length = Math.round(asNumber(indicator.params.length, 14, 2, 200));
    return Math.max(0, length);
  }
  if (indicator.type === "MACD") {
    const slow = Math.round(asNumber(indicator.params.slow, 26, 3, 200));
    const signalLength = Math.round(asNumber(indicator.params.signal, 9, 2, 100));
    return Math.max(0, slow + signalLength - 2);
  }
  if (indicator.type === "BBANDS") {
    const length = Math.round(asNumber(indicator.params.length, 20, 2, 300));
    return Math.max(0, length - 1);
  }
  return 0;
}

/**
 * Schätzt den gesamten Warmup-Index für alle konfigurierten Indikatoren und die KI.
 * @param {Array<object>} indicators - Die Liste der konfigurierten Indikatoren.
 * @param {boolean} useMlVote - Gibt an, ob die KI-Vorhersage verwendet wird.
 * @returns {number} Der maximale Warmup-Index.
 */
export function estimateWarmupIndex(indicators, useMlVote) {
  let index = 0;
  for (const indicator of indicators) {
    index = Math.max(index, minIndexForIndicator(indicator));
  }
  if (useMlVote) {
    // Berücksichtigt die Warmup-Perioden für die ML-Features (SMA und RSI).
    index = Math.max(index, Math.max(ML_CONFIG.smaPeriod - 1, ML_CONFIG.rsiPeriod));
  }
  return index;
}

/**
 * Evaluiert das Signal eines einzelnen Indikators für eine bestimmte Kerze.
 * @param {object} indicator - Das Indikator-Konfigurationsobjekt.
 * @param {Array<object>} candles - Die Candlestick-Daten.
 * @param {number} index - Der Index der Kerze.
 * @returns {object} Ein Objekt mit `ready`, `signal`, `valueText` und `minIndex`.
 */
function voteByIndicator(indicator, candles, index) {
  const close = candles[index].close;

  if (indicator.type === "SMA") {
    const length = Math.round(asNumber(indicator.params.length, 20, 2, 300));
    const sma = calcSma(candles, index, length);
    if (sma == null) {
      return { ready: false, minIndex: length - 1 };
    }
    return {
      ready: true,
      signal: signalFromCompare(close, sma),
      valueText: `Close ${close.toFixed(2)} / SMA ${sma.toFixed(2)}`
    };
  }

  if (indicator.type === "EMA") {
    const length = Math.round(asNumber(indicator.params.length, 21, 2, 300));
    const ema = calcEma(candles, index, length);
    if (ema == null) {
      return { ready: false, minIndex: length - 1 };
    }
    return {
      ready: true,
      signal: signalFromCompare(close, ema),
      valueText: `Close ${close.toFixed(2)} / EMA ${ema.toFixed(2)}`
    };
  }

  if (indicator.type === "RSI") {
    const length = Math.round(asNumber(indicator.params.length, 14, 2, 200));
    const overbought = asNumber(indicator.params.overbought, 70, 50, 95);
    const oversold = asNumber(indicator.params.oversold, 30, 5, 50);
    const rsi = calcRsi(candles, index, length);
    if (rsi == null) {
      return { ready: false, minIndex: length };
    }
    let signal = "HOLD";
    if (rsi <= oversold) {
      signal = "BUY";
    } else if (rsi >= overbought) {
      signal = "SELL";
    }
    return {
      ready: true,
      signal,
      valueText: `RSI ${rsi.toFixed(2)}`
    };
  }

  if (indicator.type === "MACD") {
    const fast = Math.round(asNumber(indicator.params.fast, 12, 2, 100));
    const slow = Math.round(asNumber(indicator.params.slow, 26, 3, 200));
    const signalLength = Math.round(asNumber(indicator.params.signal, 9, 2, 100));
    const macd = calcMacd(candles, index, fast, slow, signalLength);
    if (macd == null) {
      return { ready: false, minIndex: slow + signalLength - 2 };
    }
    return {
      ready: true,
      signal: signalFromCompare(macd.macd, macd.signal),
      valueText: `MACD ${macd.macd.toFixed(4)} / Sig ${macd.signal.toFixed(4)}`
    };
  }

  if (indicator.type === "BBANDS") {
    const length = Math.round(asNumber(indicator.params.length, 20, 2, 300));
    const mult = asNumber(indicator.params.mult, 2, 0.5, 5);
    const bbands = calculateBbands(candles, index, length, mult);
    if (bbands == null) {
      return { ready: false, minIndex: length - 1 };
    }
    const upper = bbands.upper;
    const lower = bbands.lower;
    let signal = "HOLD";
    if (close < lower) {
      signal = "BUY";
    } else if (close > upper) {
      signal = "SELL";
    }
    return {
      ready: true,
      signal,
      valueText: `Close ${close.toFixed(2)} / BB ${lower.toFixed(2)}-${upper.toFixed(2)}`
    };
  }

  return { ready: false, minIndex: 0 };
}

/**
 * Generiert eine eindeutige ID für einen neuen Indikator.
 * @returns {string} Eine eindeutige ID.
 */
function createId() {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }
  return `ind-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
}

/**
 * Erstellt ein neues Indikator-Konfigurationsobjekt basierend auf einer Vorlage.
 * @param {string} type - Der Typ des zu erstellenden Indikators.
 * @returns {object|null} Das neue Indikatorobjekt oder null, wenn der Typ unbekannt ist.
 */
export function createIndicator(type) {
  const template = templateByType.get(type);
  if (!template) {
    return null;
  }
  return {
    id: createId(),
    type,
    label: template.label,
    params: { ...template.defaults }
  };
}

/**
 * Sanitisiert einen Parameterwert basierend auf der Indikatorvorlage.
 * Stellt sicher, dass der Wert numerisch ist und innerhalb der definierten Grenzen liegt.
 * @param {string} indicatorType - Der Typ des Indikators.
 * @param {string} key - Der Schlüssel des Parameters.
 * @param {*} value - Der zu sanitierende Wert.
 * @returns {number} Der sanitierte numerische Wert.
 */
export function sanitizeParam(indicatorType, key, value) {
  const template = templateByType.get(indicatorType);
  if (!template) {
    return value;
  }
  const field = template.fields.find((item) => item.key === key);
  if (!field) {
    return value;
  }
  const fallback = template.defaults[key];
  const n = asNumber(value, fallback, field.min, field.max);
  if (field.step >= 1) {
    return Math.round(n);
  }
  return Number(n.toFixed(4));
}

/**
 * Evaluiert die Signale aller konfigurierten Indikatoren für eine bestimmte Kerze.
 * @param {Array<object>} candles - Die Candlestick-Daten.
 * @param {number} index - Der Index der Kerze.
 * @param {Array<object>} indicators - Die Liste der konfigurierten Indikatoren.
 * @returns {object} Ein Objekt mit `ready`, `minReadyIndex` und einer Liste von `votes`.
 */
export function evaluateIndicatorVotes(candles, index, indicators) {
  let minReadyIndex = 0;
  const votes = [];
  for (const indicator of indicators) {
    const result = voteByIndicator(indicator, candles, index);
    minReadyIndex = Math.max(minReadyIndex, result.minIndex ?? 0);
    if (!result.ready) {
      // Wenn ein Indikator nicht bereit ist, ist die gesamte Evaluierung nicht bereit.
      return { ready: false, minReadyIndex, votes: [] };
    }
    votes.push({
      id: indicator.id,
      name: indicator.label,
      signal: result.signal,
      valueText: result.valueText
    });
  }
  return { ready: true, minReadyIndex, votes };
}

/**
 * Leitet ein Handelssignal aus einer KI-Vorhersage ab.
 * @param {object} prediction - Das KI-Vorhersageobjekt ({ label: string, confidence: number }).
 * @param {number} [threshold=ML_CONFIG.confidenceThreshold] - Der Konfidenzschwellenwert.
 * @returns {string} "BUY", "SELL" oder "HOLD".
 */
export function mlVoteFromPrediction(prediction, threshold = ML_CONFIG.confidenceThreshold) {
  if (!prediction || prediction.confidence == null || prediction.label == null) {
    return "HOLD";
  }
  if (prediction.confidence < threshold) {
    return "HOLD";
  }
  // Abgleich mit den Labels vom Python ML-Service (UP/DOWN oder STEIGT/FÄLLT)
  const label = String(prediction.label).toUpperCase();
  if (label === "UP" || label === "STEIGT") {
    return "BUY";
  }
  if (label === "DOWN" || label === "FÄLLT") {
    return "SELL";
  }
  return "HOLD";
}

/**
 * Bildet einen Konsens aus den Stimmen der Indikatoren.
 * @param {Array<object>} votes - Eine Liste von Indikator-Votes ({ id, name, signal, valueText }).
 * @param {boolean} requireMl - Gibt an, ob die KI-Stimme für den Konsens erforderlich ist.
 * @returns {object} Ein Objekt { signal, reason }.
 */
export function buildConsensus(votes, requireMl) {
  // Filtert relevante Stimmen basierend auf dem 'requireMl'-Flag.
  const relevant = votes.filter((vote) => (requireMl ? true : vote.id !== "ml-vote"));

  if (!relevant.length) {
    return {
      signal: "HOLD",
      reason: "No indicators configured."
    };
  }

  // Prüft, ob alle relevanten Stimmen übereinstimmen.
  const first = relevant[0].signal;
  if (first === "HOLD") {
    return {
      signal: "HOLD",
      reason: "No consensus: first vote is HOLD."
    };
  }
  const mismatch = relevant.find((vote) => vote.signal !== first);
  if (mismatch) {
    return {
      signal: "HOLD",
      reason: `No consensus: ${mismatch.name}=${mismatch.signal}.`
    };
  }

  // Wenn alle Stimmen übereinstimmen, wird ein Konsens-Signal zurückgegeben.
  return {
    signal: first,
    reason: `Consensus ${first}: ${relevant.map((vote) => `${vote.name}=${vote.signal}`).join(", ")}`
  };
}

/**
 * Baut den Feature-Vektor für die KI-Vorhersage.
 * @param {Array<object>} candles - Die Candlestick-Daten.
 * @param {number} index - Der Index der Kerze.
 * @returns {object|null} Ein Objekt mit `features`, `close`, `sma`, `rsi` oder null, wenn nicht genügend Daten vorhanden sind.
 */
export function buildMlFeatures(candles, index) {
  const close = candles[index].close;
  // Berechnet SMA und RSI für die ML-Features (basierend auf ML_CONFIG).
  const sma = calcSma(candles, index, ML_CONFIG.smaPeriod);
  const rsi = calcRsi(candles, index, ML_CONFIG.rsiPeriod);

  if (sma == null || rsi == null) {
    return null;
  }
  // Der Feature-Vektor muss die gleiche Reihenfolge wie im Python-ML-Service haben.
  const features = [close, sma, rsi, close - sma];

  // Prüft auf NaN-Werte in den Features.
  if (features.some((value) => !Number.isFinite(value))) {
    return null;
  }
  return {
    features,
    close,
    sma,
    rsi
  };
}
