import { useEffect, useRef } from "react";
import { createChart } from "lightweight-charts";

// --- Hilfsfunktionen für Indikatorberechnungen im Frontend ---

/**
 * Berechnet den Simple Moving Average (SMA) für eine Reihe von Candlestick-Daten.
 * @param {Array<object>} data - Die Candlestick-Daten, wobei jedes Objekt ein 'close'-Feld und ein 'time'-Feld hat.
 * @param {number} period - Die Periode für die SMA-Berechnung.
 * @returns {Array<object>} Eine Liste von Objekten { time, value } für die SMA-Linie.
 */
function calculateSma(data, period) {
  const result = [];
  for (let i = 0; i < data.length; i++) {
    // SMA benötigt mindestens 'period' Datenpunkte.
    if (i < period - 1) continue;
    const avg = data.slice(i - period + 1, i + 1).reduce((sum, c) => sum + c.close, 0) / period;
    result.push({ time: data[i].time, value: avg });
  }
  return result;
}

/**
 * Berechnet den Exponential Moving Average (EMA) für eine Reihe von Candlestick-Daten.
 * @param {Array<object>} data - Die Candlestick-Daten.
 * @param {number} period - Die Periode für die EMA-Berechnung.
 * @returns {Array<object>} Eine Liste von Objekten { time, value } für die EMA-Linie.
 */
function calculateEma(data, period) {
  const result = [];
  if (data.length < period) return result;

  // Initialer EMA-Wert (oft als SMA der ersten 'period' Kerzen)
  let ema = data.slice(0, period).reduce((sum, c) => sum + c.close, 0) / period;
  const k = 2 / (period + 1); // Glättungsfaktor

  // Berechnet den EMA für die restlichen Datenpunkte
  for (let i = period - 1; i < data.length; i++) {
    ema = data[i].close * k + ema * (1 - k);
    result.push({ time: data[i].time, value: ema });
  }
  return result;
}

/**
 * Berechnet den Relative Strength Index (RSI) für eine Reihe von Candlestick-Daten.
 * @param {Array<object>} data - Die Candlestick-Daten.
 * @param {number} period - Die Periode für die RSI-Berechnung.
 * @returns {Array<object>} Eine Liste von Objekten { time, value } für die RSI-Linie.
 */
function calculateRsi(data, period) {
  const result = [];
  if (data.length <= period) return result; // RSI benötigt 'period' + 1 Datenpunkte für die erste Berechnung

  for (let i = 1; i < data.length; i++) {
    if (i < period) continue; // Warte, bis genügend Daten für die Periode vorhanden sind

    let gains = 0, losses = 0;
    // Berechnet Gewinne und Verluste über die Periode
    for (let j = i - period + 1; j <= i; j++) {
      const diff = data[j].close - data[j - 1].close;
      if (diff >= 0) gains += diff; else losses += Math.abs(diff);
    }

    const rs = losses === 0 ? 100 : gains / losses; // Relative Stärke
    result.push({ time: data[i].time, value: 100 - (100 / (1 + rs)) }); // RSI-Formel
  }
  return result;
}

/**
 * Berechnet die Bollinger Bänder (Upper, Lower, Middle) für eine Reihe von Candlestick-Daten.
 * @param {Array<object>} data - Die Candlestick-Daten.
 * @param {number} period - Die Periode für den SMA (Middle Band) und die Standardabweichung.
 * @param {number} mult - Der Multiplikator für die Standardabweichung.
 * @returns {object} Ein Objekt mit drei Listen: { upper, lower, middle }.
 */
function calculateBbands(data, period, mult) {
    const upper = [], lower = [], middle = [];
    for (let i = 0; i < data.length; i++) {
        if (i < period - 1) continue; // BBANDS benötigt mindestens 'period' Datenpunkte

        const window = data.slice(i - period + 1, i + 1);
        const avg = window.reduce((sum, c) => sum + c.close, 0) / period; // Mittleres Band (SMA)

        const squareDiffs = window.map(c => Math.pow(c.close - avg, 2));
        const stdDev = Math.sqrt(squareDiffs.reduce((sum, d) => sum + d, 0) / period); // Standardabweichung

        upper.push({ time: data[i].time, value: avg + stdDev * mult });
        lower.push({ time: data[i].time, value: avg - stdDev * mult });
        middle.push({ time: data[i].time, value: avg });
    }
    return { upper, lower, middle };
}

/**
 * React-Komponente für die Anzeige eines interaktiven Candlestick-Charts mit dynamischen Indikatoren.
 * Nutzt die Lightweight Charts Bibliothek.
 *
 * @param {object} props - Die Props der Komponente.
 * @param {Array<object>} props.candles - Die Liste der Candlestick-Daten, die im Chart angezeigt werden sollen.
 * @param {Array<object>} [props.indicators=[]] - Eine Liste der konfigurierten Indikatoren (SMA, EMA, RSI, BBANDS).
 * @param {boolean} [props.replayEnabled=false] - Gibt an, ob der Replay-Modus aktiv ist.
 * @param {number} [props.replayIndex=0] - Der aktuelle Index im Replay-Modus, bis zu dem Daten angezeigt werden sollen.
 * @param {Array<object>} [props.replayMarkers=[]] - Marker für Trades im Replay-Modus.
 * @returns {JSX.Element} Das gerenderte Chart-Panel.
 */
export default function CandlesChart({
  candles,
  indicators = [],
  replayEnabled = false,
  replayIndex = 0,
  replayMarkers = []
}) {
  const containerRef = useRef(null); // Referenz auf das DOM-Element, das das Chart hostet

  /**
   * useEffect Hook zur Initialisierung und Aktualisierung des Charts.
   * Wird bei Änderungen an 'candles', 'indicators', 'replayEnabled', 'replayIndex' oder 'replayMarkers' ausgeführt.
   */
  useEffect(() => {
    // Chart nur initialisieren, wenn der Container existiert und Daten vorhanden sind.
    if (!containerRef.current || candles.length === 0) return;

    // Erstellt eine neue Chart-Instanz
    const chart = createChart(containerRef.current, {
      width: containerRef.current.clientWidth,
      height: 500, // Feste Höhe des Charts
      layout: { background: { color: "#0f172a" }, textColor: "#cbd5e1" }, // Farbschema
      grid: { vertLines: { color: "#1e293b" }, horzLines: { color: "#1e293b" } }, // Gitterlinien
      timeScale: { borderColor: "#334155" }, // Zeitachsen-Stil
      leftPriceScale: { visible: true, borderColor: "#334155" }, // Linke Preisskala für Oszillatoren (z.B. RSI)
      rightPriceScale: { visible: true, borderColor: "#334155" } // Rechte Preisskala für Preisdaten (Kerzen, SMA, EMA, BBANDS)
    });

    // Fügt die Candlestick-Serie zum Chart hinzu
    const candleSeries = chart.addCandlestickSeries({
      upColor: "#16a34a", downColor: "#dc2626", // Farben für steigende/fallende Kerzen
      wickUpColor: "#22c55e", wickDownColor: "#ef4444", // Farben für Dochte
      borderVisible: false // Keine Rahmen um die Kerzen
    });

    // Mappt die rohen Candle-Daten in das von Lightweight Charts erwartete Format
    const mapped = candles.map((c) => ({
      time: Math.floor(new Date(c.timestamp).getTime() / 1000), // Zeitstempel in Sekunden
      open: c.open, high: c.high, low: c.low, close: c.close
    }));

    // Bestimmt die sichtbaren Daten basierend auf dem Replay-Modus
    const visibleData = replayEnabled ? mapped.slice(0, replayIndex + 1) : mapped;
    candleSeries.setData(visibleData);

    // --- DYNAMISCHE INDIKATOREN ZEICHNEN ---
    // Farbpalette für die Indikatorlinien
    const colorPalette = ["#f59e0b", "#3b82f6", "#ec4899", "#8b5cf6", "#10b981", "#ef4444", "#fbbf24"];

    indicators.forEach((ind, index) => {
      const color = colorPalette[index % colorPalette.length]; // Zyklische Farbauswahl

      // SMA (Simple Moving Average)
      if (ind.type === "SMA") {
        const series = chart.addLineSeries({ color, lineWidth: 2, title: `SMA ${ind.params.length}` });
        series.setData(calculateSma(visibleData, Number(ind.params.length)));
      }
      // EMA (Exponential Moving Average)
      else if (ind.type === "EMA") {
        const series = chart.addLineSeries({ color, lineWidth: 2, lineStyle: 2, title: `EMA ${ind.params.length}` });
        series.setData(calculateEma(visibleData, Number(ind.params.length)));
      }
      // RSI (Relative Strength Index) - wird auf der linken Preisskala gezeichnet
      else if (ind.type === "RSI") {
        const rsiSeries = chart.addLineSeries({
            color: "#8b5cf6", // Feste Farbe für RSI
            lineWidth: 2,
            priceScaleId: 'left', // Zuweisung zur linken Preisskala
            title: `RSI ${ind.params.length}`
        });
        rsiSeries.setData(calculateRsi(visibleData, Number(ind.params.length)));

        // Horizontale Linien für Overbought/Oversold-Bereiche
        rsiSeries.createPriceLine({ price: Number(ind.params.overbought), color: "#ef4444", lineStyle: 3, axisLabelVisible: true, title: 'OB' });
        rsiSeries.createPriceLine({ price: Number(ind.params.oversold), color: "#22c55e", lineStyle: 3, axisLabelVisible: true, title: 'OS' });
      }
      // BBANDS (Bollinger Bänder) - bestehen aus drei Linien
      else if (ind.type === "BBANDS") {
        const { upper, lower, middle } = calculateBbands(visibleData, Number(ind.params.length), Number(ind.params.mult));
        // Obere Band
        const upS = chart.addLineSeries({ color: "#60a5fa", lineWidth: 1, lineStyle: 2, title: 'BB Upper' });
        // Untere Band
        const loS = chart.addLineSeries({ color: "#60a5fa", lineWidth: 1, lineStyle: 2, title: 'BB Lower' });
        // Mittleres Band (SMA)
        const miS = chart.addLineSeries({ color: "#94a3b8", lineWidth: 1, title: 'BB Basis' });
        upS.setData(upper);
        loS.setData(lower);
        miS.setData(middle);
      }
      // Hier könnten weitere Indikatoren wie MACD hinzugefügt werden (würden ein eigenes Pane benötigen)
    });

    // --- Trades Markieren ---
    if (replayEnabled) {
      const markers = replayMarkers
        .filter((m) => m.index <= replayIndex && m.signal !== "HOLD") // Nur Marker bis zum aktuellen Replay-Index anzeigen
        .map((m) => ({
          time: mapped[m.index].time,
          position: m.signal === "BUY" ? "belowBar" : "aboveBar", // Position des Markers relativ zur Kerze
          color: m.signal === "BUY" ? "#22c55e" : "#ef4444", // Farbe des Markers
          shape: m.signal === "BUY" ? "arrowUp" : "arrowDown", // Form des Markers
          text: m.signal // Text des Markers
        }));
      candleSeries.setMarkers(markers);
    }

    chart.timeScale().fitContent(); // Passt die Zeitachse an die Daten an

    // Event Listener für die Größenänderung des Fensters, um das Chart anzupassen
    const onResize = () => chart.applyOptions({ width: containerRef.current.clientWidth });
    window.addEventListener("resize", onResize);

    // Cleanup-Funktion: Entfernt Event Listener und zerstört das Chart, wenn die Komponente unmounted wird
    return () => {
      window.removeEventListener("resize", onResize);
      chart.remove();
    };
  }, [candles, indicators, replayEnabled, replayIndex, replayMarkers]); // Dependencies des useEffect Hooks

  // --- JSX Rendering ---
  return (
    <section className="panel">
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '10px' }}>
        <h2>Markt-Chart (mit dynamischen Indikatoren)</h2>
        <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
          Linke Achse: Oszillatoren (RSI) | Rechte Achse: Preis
        </div>
      </div>
      <div ref={containerRef} className="chart-wrap" /> {/* Container für das Lightweight Chart */}
    </section>
  );
}
