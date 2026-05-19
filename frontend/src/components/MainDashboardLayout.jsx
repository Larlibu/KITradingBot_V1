import React from 'react';
import ControlsPanel from './ControlsPanel';
import MlTrainingPanel from './MlTrainingPanel';
import IndicatorConfigPanel from './IndicatorConfigPanel';
import CandlesChart from './CandlesChart';
// TradesTable wird direkt in App.jsx gerendert, nicht hier

/**
 * React-Komponente für das Hauptlayout des Dashboards.
 * Organisiert die Sidebar und den Hauptinhaltsbereich,
 * und rendert die entsprechenden Panels basierend auf dem Ansichtsmodus.
 *
 * @param {object} props - Die Props der Komponente.
 * @param {boolean} props.isLive - Gibt an, ob der Live-Modus aktiv ist.
 * @param {object} props.signalSnapshot - Der aktuelle SignalSnapshot vom Backend.
 * @param {Function} props.handleStartTrading - Callback zum Starten des Handels.
 * @param {Function} props.handleStopTrading - Callback zum Stoppen des Handels.
 * @param {boolean} props.useMlVote - Gibt an, ob der KI-Konsens aktiviert ist.
 * @param {Function} props.setUseMlVote - Callback zum Umschalten des KI-Konsens.
 * @param {Array<object>} props.indicators - Liste der konfigurierten Indikatoren.
 * @param {Array<object>} props.indicatorTemplates - Vorlagen für Indikatoren.
 * @param {string} props.selectedIndicatorType - Der aktuell ausgewählte Indikatortyp.
 * @param {Function} props.setSelectedIndicatorType - Callback zum Setzen des ausgewählten Indikatortyps.
 * @param {Function} props.addIndicator - Callback zum Hinzufügen eines Indikators.
 * @param {Function} props.removeIndicator - Callback zum Entfernen eines Indikators.
 * @param {Function} props.changeIndicatorParam - Callback zum Ändern von Indikatorparametern.
 * @param {Array<object>} props.marketData - Die Marktdaten für das Chart.
 * @param {boolean} props.replayEnabled - Gibt an, ob der Replay-Modus aktiv ist.
 * @param {number} props.replayIndex - Der aktuelle Replay-Index.
 * @param {Array<object>} props.replayMarkers - Marker für das Replay-Chart.
 * @param {object|null} props.backtest - Die Backtest-Ergebnisse.
 * @param {boolean} props.loadingBacktest - Ladezustand des Backtests.
 * @param {Function} props.handleBacktest - Callback zum Ausführen des Backtests.
 * @param {Function} props.setReplayEnabled - Callback zum Umschalten des Replay-Modus.
 * @param {Function} props.handleReplayStep - Callback zum Navigieren im Replay.
 * @param {number} props.maxReplayIndex - Maximaler Replay-Index.
 * @param {boolean} props.isPlaying - Gibt an, ob das Replay abgespielt wird.
 * @param {Function} props.togglePlay - Callback zum Starten/Pausieren des Replays.
 * @param {number} props.playbackSpeed - Aktuelle Wiedergabegeschwindigkeit.
 * @param {Function} props.setPlaybackSpeed - Callback zum Setzen der Wiedergabegeschwindigkeit.
 * @param {object|null} props.replaySignal - Das aktuelle Replay-Signal.
 * @returns {JSX.Element} Das gerenderte Layout.
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
  // chartSmaPeriod wurde entfernt, da es nicht mehr direkt benötigt wird
  replayEnabled,
  replayIndex,
  replayMarkers,
  backtest,
  loadingBacktest,
  handleBacktest,
  setReplayEnabled,
  handleReplayStep,
  maxReplayIndex,
  isPlaying,
  togglePlay,
  playbackSpeed,
  setPlaybackSpeed,
  replaySignal,
}) {
  // Hilfsfunktion zur Umwandlung eines Signals in ein lesbares Positionslabel.
  const toPositionLabel = (signal) => {
    if (signal === "BUY") return "LONG";
    if (signal === "SELL") return "SHORT";
    return "WARTEN";
  };

  return (
    <div className="dashboard-layout">
      {/* Sidebar für Steuerung und Konfiguration */}
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

      {/* Hauptinhaltsbereich */}
      <section className="main-content">
        {/* Candlestick-Chart */}
        <div className="panel chart-panel">
          <CandlesChart
            candles={marketData}
            indicators={indicators} // Übergabe der dynamischen Indikatoren an das Chart
            replayEnabled={replayEnabled && !isLive}
            replayIndex={replayIndex}
            replayMarkers={replayMarkers}
            // smaPeriod wurde entfernt, da CandlesChart die 'indicators' direkt nutzt
          />
        </div>

        {/* Replay-Panel nur im Analyse-Modus */}
        {!isLive && (
          <section className="panel replay-panel">
            <div className="panel-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <h2>Analyse & Replay</h2>
              <div style={{ display: 'flex', gap: '10px' }}>
                <button className="warning-button" onClick={() => handleBacktest()} disabled={loadingBacktest}>
                  {loadingBacktest ? "Läuft..." : "Schnell-Analyse (Backtest)"}
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
              <div className="backtest-summary" style={{ margin: '15px 0', padding: '15px', background: 'rgba(0,0,0,0.2)', borderRadius: '8px', display: 'flex', gap: '30px', justifyContent: 'center' }}>
                <div>Rendite: <strong style={{ color: backtest.strategyReturnPct >= 0 ? 'var(--success)' : 'var(--danger)' }}>{backtest.strategyReturnPct.toFixed(2)}%</strong></div>
                <div>Trades: <strong>{backtest.totalTrades}</strong></div>
                <div>Win-Rate: <strong>{backtest.winRate.toFixed(1)}%</strong></div>
              </div>
            )}

            {replayEnabled && (
              <div className="replay-controls">
                <div className="control-row">
                  <button onClick={() => handleReplayStep(-1)} disabled={replayIndex <= 0}>←</button>
                  <button className={isPlaying ? "warning-button" : "success-button"} onClick={togglePlay}>
                    {isPlaying ? "⏸ Pause" : "▶ Abspielen"}
                  </button>
                  <button onClick={() => handleReplayStep(1)} disabled={replayIndex >= maxReplayIndex}>→</button>
                </div>
                <div className="speed-control">
                  <input
                    type="range"
                    min="100"
                    max="2000"
                    step="100"
                    value={2100 - playbackSpeed}
                    onChange={(e) => setPlaybackSpeed(e.target.value)} // Korrigiert: e.target.value direkt verwenden
                  />
                  <div style={{ textAlign: 'center', fontSize: '0.8rem', color: 'var(--text-muted)' }}>Geschwindigkeit</div>
                </div>
                <p className="replay-status">Kerze {replayIndex + 1} / {marketData.length}</p>

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
