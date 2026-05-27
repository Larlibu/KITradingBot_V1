import React from "react";
import ControlsPanel from "./ControlsPanel";
import MlTrainingPanel from "./MlTrainingPanel";
import IndicatorConfigPanel from "./IndicatorConfigPanel";
import CandlesChart from "./CandlesChart";

/**
 * React-Komponente fuer das Hauptlayout des Dashboards.
 * Organisiert die Sidebar und den Hauptinhaltsbereich.
 */
export default function MainDashboardLayout({
  isLive,
  signalSnapshot,
  handleStartTrading,
  handleStopTrading,
  useMlVote,
  setUseMlVote,
  indicators,
  indicatorTemplates,
  selectedIndicatorType,
  setSelectedIndicatorType,
  addIndicator,
  removeIndicator,
  changeIndicatorParam,
  marketData,
  replayEnabled,
  replayIndex,
  replayMarkers,
  backtest,
  loadingBacktest,
  handleBacktest,
  setReplayEnabled,
  handleReplayStep,
  goToReplayIndex,
  maxReplayIndex,
  isPlaying,
  togglePlay,
  playbackSpeed,
  setPlaybackSpeed,
  replaySignal,
}) {
  const toPositionLabel = (signal) => {
    if (signal === "BUY") return "LONG";
    if (signal === "SELL") return "SHORT";
    return "WARTEN";
  };

  return (
    <div className="dashboard-layout">
      <aside className="sidebar">
        {isLive && (
          <ControlsPanel
            tradingEnabled={signalSnapshot?.tradingEnabled ?? false}
            onStart={handleStartTrading}
            onStop={handleStopTrading}
            useMlVote={useMlVote}
            setUseMlVote={setUseMlVote}
          />
        )}

        {!isLive && (
          <>
            <MlTrainingPanel />
            <IndicatorConfigPanel
              indicators={indicators}
              templates={indicatorTemplates}
              selectedType={selectedIndicatorType}
              setSelectedType={setSelectedIndicatorType}
              onAdd={addIndicator}
              onRemove={removeIndicator}
              onParamChange={changeIndicatorParam}
              useMlVote={useMlVote}
              setUseMlVote={setUseMlVote}
            />
          </>
        )}
      </aside>

      <section className="main-content">
        <div className="panel chart-panel">
          <CandlesChart
            candles={marketData}
            indicatorHistory={backtest?.indicatorHistory} 
            replayEnabled={replayEnabled && !isLive}
            replayIndex={replayIndex}
            replayMarkers={replayMarkers}
          />
        </div>

        {!isLive && (
          <section className="panel replay-panel">
            <div className="panel-header" style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <h2>Analyse & Replay</h2>
              <div style={{ display: "flex", gap: "10px" }}>
                <button className="warning-button" onClick={() => handleBacktest()} disabled={loadingBacktest}>
                  {loadingBacktest ? "Laeuft..." : "Schnell-Analyse (Backtest)"}
                </button>
                <button
                  className={replayEnabled ? "secondary" : "primary-button"}
                  onClick={() => setReplayEnabled(!replayEnabled)}
                >
                  {replayEnabled ? "Replay beenden" : "Visuelles Replay"}
                </button>
              </div>
            </div>

            {backtest && !replayEnabled && (
              <div className="backtest-summary" style={{ margin: "15px 0", padding: "15px", background: "rgba(0,0,0,0.2)", borderRadius: "8px", display: "flex", gap: "30px", justifyContent: "center" }}>
                <div>Rendite: <strong style={{ color: backtest.strategyReturnPct >= 0 ? "var(--success)" : "var(--danger)" }}>{backtest.strategyReturnPct.toFixed(2)}%</strong></div>
                <div>Trades: <strong>{backtest.totalTrades}</strong></div>
                <div>Win-Rate: <strong>{backtest.winRate.toFixed(1)}%</strong></div>
              </div>
            )}

            {replayEnabled && (
              <div className="replay-controls">
                <div className="control-row">
                  <button onClick={() => handleReplayStep(-1)} disabled={replayIndex <= 0}>{"\u2190"}</button>
                  <button className={isPlaying ? "warning-button" : "success-button"} onClick={togglePlay}>
                    {isPlaying ? "\u23F8 Pause" : "\u25B6 Abspielen"}
                  </button>
                  <button onClick={() => handleReplayStep(1)} disabled={replayIndex >= maxReplayIndex}>{"\u2192"}</button>
                </div>
                
                <div className="navigation-control" style={{ margin: "10px 0" }}>
                   <input
                    type="range"
                    min="0"
                    max={maxReplayIndex}
                    step="1"
                    value={replayIndex}
                    onChange={(e) => goToReplayIndex(Number(e.target.value))}
                    style={{ width: "100%" }}
                  />
                  <div style={{ textAlign: "center", fontSize: "0.8rem", color: "var(--text-muted)" }}>Position (Kerze {replayIndex + 1} / {marketData.length})</div>
                </div>

                <div className="speed-control">
                  <input
                    type="range"
                    min="100"
                    max="2000"
                    step="100"
                    value={2100 - playbackSpeed}
                    onChange={(e) => setPlaybackSpeed(2100 - Number(e.target.value))}
                  />
                  <div style={{ textAlign: "center", fontSize: "0.8rem", color: "var(--text-muted)" }}>Geschwindigkeit</div>
                </div>

                {replaySignal?.status === "READY" && (
                  <div className="replay-result">
                    <div className={`signal-badge signal-${replaySignal.decision.signal.toLowerCase()}`}>
                      {toPositionLabel(replaySignal.decision.signal)}
                    </div>
                    <p className="reason-text">{replaySignal.decision.reason}</p>
                  </div>
                )}
              </div>
            )}
          </section>
        )}
      </section>
    </div>
  );
}
