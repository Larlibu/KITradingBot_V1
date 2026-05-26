import { useEffect, useRef } from "react";
import { createChart } from "lightweight-charts";

/**
 * React-Komponente fuer die Anzeige eines interaktiven Candlestick-Charts.
 */
export default function CandlesChart({
  candles,
  indicatorHistory = [],
  replayEnabled = false,
  replayIndex = 0,
  replayMarkers = []
}) {
  const containerRef = useRef(null);

  useEffect(() => {
    if (!containerRef.current || !candles || candles.length === 0) return;

    const chart = createChart(containerRef.current, {
      width: containerRef.current.clientWidth,
      height: 500,
      layout: { background: { color: "#0f172a" }, textColor: "#cbd5e1" },
      grid: { vertLines: { color: "#1e293b" }, horzLines: { color: "#1e293b" } },
      timeScale: { borderColor: "#334155" },
      leftPriceScale: { visible: true, borderColor: "#334155" },
      rightPriceScale: { visible: true, borderColor: "#334155" }
    });

    const candleSeries = chart.addCandlestickSeries({
      upColor: "#16a34a", downColor: "#dc2626",
      wickUpColor: "#22c55e", wickDownColor: "#ef4444",
      borderVisible: false
    });

    // Sicherheit: Falls Zeitstempel fehlt
    const mapped = candles.map((c) => ({
      time: c.timestamp ? Math.floor(new Date(c.timestamp).getTime() / 1000) : 0,
      open: c.open, high: c.high, low: c.low, close: c.close
    }));

    const visibleData = replayEnabled ? mapped.slice(0, replayIndex + 1) : mapped;
    candleSeries.setData(visibleData);

    // --- TA4J INDIKATOREN VOM BACKEND ZEICHNEN ---
    if (indicatorHistory && indicatorHistory.length > 0) {
      const colorPalette = ["#f59e0b", "#3b82f6", "#ec4899", "#8b5cf6", "#10b981", "#ef4444", "#fbbf24"];
      
      const indMap = {};
      const limit = Math.min(indicatorHistory.length, mapped.length); 
      const currentLimit = replayEnabled ? Math.min(replayIndex + 1, limit) : limit;

      for (let i = 0; i < currentLimit; i++) {
        const step = indicatorHistory[i];
        if (!step) continue;
        step.forEach(iv => {
          if (!indMap[iv.name]) indMap[iv.name] = [];
          indMap[iv.name].push({ time: mapped[i].time, value: iv.value });
        });
      }

      Object.keys(indMap).forEach((name, idx) => {
        const color = colorPalette[idx % colorPalette.length];
        const isRsi = name.toUpperCase().includes("RSI");
        const isBollinger = name.toUpperCase().includes("BB");

        const series = chart.addLineSeries({
          color,
          lineWidth: isBollinger ? 1 : 2,
          lineStyle: isBollinger ? 2 : 0,
          priceScaleId: isRsi ? "left" : "right",
          title: name
        });
        series.setData(indMap[name]);
      });
    }

    // --- Trades Markieren ---
    const markersToUse = replayEnabled ? replayMarkers.filter(m => m.index <= replayIndex) : replayMarkers;
    const markers = markersToUse
        .filter((m) => m.signal !== "HOLD" && mapped[m.index])
        .map((m) => ({
          time: mapped[m.index].time,
          position: m.signal === "BUY" ? "belowBar" : "aboveBar",
          color: m.signal === "BUY" ? "#22c55e" : "#ef4444",
          shape: m.signal === "BUY" ? "arrowUp" : "arrowDown",
          text: m.signal
        }));
    candleSeries.setMarkers(markers);

    chart.timeScale().fitContent();

    const onResize = () => {
        if (containerRef.current) {
            chart.applyOptions({ width: containerRef.current.clientWidth });
        }
    };
    window.addEventListener("resize", onResize);

    return () => {
      window.removeEventListener("resize", onResize);
      chart.remove();
    };
  }, [candles, indicatorHistory, replayEnabled, replayIndex, replayMarkers]);

  return (
    <section className="panel">
      <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "10px" }}>
        <h2>Markt-Chart (ta4j Backend-Daten)</h2>
        <div style={{ fontSize: "0.8rem", color: "var(--text-muted)" }}>
          Linke Achse: RSI | Rechte Achse: Preis & Trends
        </div>
      </div>
      <div ref={containerRef} className="chart-wrap" />
    </section>
  );
}
