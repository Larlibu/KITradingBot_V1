/**
 * @file Konfigurationen fuer technische Indikatoren im Frontend.
 *       Alle Berechnungen wurden in das Java-Backend (ta4j) ausgelagert.
 *       Dieses File dient nur noch der UI-Konfiguration.
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

const templateByType = new Map(INDICATOR_TEMPLATES.map((item) => [item.type, item]));

function asNumber(value, fallback, min = -1000000, max = 1000000) {
  const n = Number(value);
  if (!Number.isFinite(n)) return fallback;
  return Math.max(min, Math.min(max, n));
}

export function createIndicator(type) {
  const template = templateByType.get(type);
  if (!template) return null;
  return {
    id: `ind-${Date.now()}-${Math.floor(Math.random() * 100000)}`,
    type,
    label: template.label,
    params: { ...template.defaults }
  };
}

export function sanitizeParam(indicatorType, key, value) {
  const template = templateByType.get(indicatorType);
  if (!template) return value;
  const field = template.fields.find((item) => item.key === key);
  if (!field) return value;
  const fallback = template.defaults[key];
  const n = asNumber(value, fallback, field.min, field.max);
  return field.step >= 1 ? Math.round(n) : Number(n.toFixed(4));
}

export function estimateWarmupIndex(indicators) {
  return 30; // Pauschaler Warmup-Wert fuer das Frontend-UI
}
