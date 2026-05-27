import AppHeader from "./AppHeader";
import AppFooter from "./AppFooter";
import MainDashboardLayout from "./MainDashboardLayout";
import DashboardCards from "./DashboardCards";
import TradesTable from "./TradesTable";
import TradeSignalModal from "./TradeSignalModal";

/**
 * Reine View-Komponente für die Startseite.
 * Erwartet ein fertiges ViewModel aus useAppLogic.
 *
 * @param {object} props - Aggregiertes ViewModel aus dem App-Controller.
 * @returns {JSX.Element} Gerenderte Hauptansicht.
 */
export default function AppView({
  viewMode,
  setViewMode,
  isLive,
  lastTradeSignal,
  resetTradeSignal,
  currentPrice,
  currentSignal,
  currentConfidence,
  currentTimestamp,
  activeIndicatorValues,
  serverStatus,
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
  backtestTrades,
  liveTrades
}) {
  return (
    <main className={`app-shell mode-${viewMode.toLowerCase()}`}>
      {/* Kopfbereich inkl. Moduswechsel */}
      <AppHeader isLive={isLive} onViewModeChange={setViewMode} />

      {/* Modal für Replay-Trade-Signale */}
      <TradeSignalModal signal={lastTradeSignal} onContinue={resetTradeSignal} />

      {/* KPI-/Status-Karten */}
      <DashboardCards
        price={Number(currentPrice)}
        signal={currentSignal}
        confidence={Number(currentConfidence)}
        indicators={activeIndicatorValues}
        serverStatus={serverStatus}
        viewMode={viewMode}
        timestamp={currentTimestamp}
      />

      {/* Hauptbereich: Sidebar/Chart/Replay */}
      <MainDashboardLayout
        isLive={isLive}
        signalSnapshot={signalSnapshot}
        handleStartTrading={handleStartTrading}
        handleStopTrading={handleStopTrading}
        useMlVote={useMlVote}
        setUseMlVote={setUseMlVote}
        indicators={indicators}
        indicatorTemplates={indicatorTemplates}
        selectedIndicatorType={selectedIndicatorType}
        setSelectedIndicatorType={setSelectedIndicatorType}
        addIndicator={addIndicator}
        removeIndicator={removeIndicator}
        changeIndicatorParam={changeIndicatorParam}
        marketData={marketData}
        replayEnabled={replayEnabled}
        replayIndex={replayIndex}
        replayMarkers={replayMarkers}
        backtest={backtest}
        loadingBacktest={loadingBacktest}
        handleBacktest={handleBacktest}
        setReplayEnabled={setReplayEnabled}
        handleReplayStep={handleReplayStep}
        goToReplayIndex={goToReplayIndex}
        maxReplayIndex={maxReplayIndex}
        isPlaying={isPlaying}
        togglePlay={togglePlay}
        playbackSpeed={playbackSpeed}
        setPlaybackSpeed={setPlaybackSpeed}
        replaySignal={replaySignal}
        backtestTrades={backtestTrades}
        liveTrades={liveTrades}
      />

      {/* Trade-Tabelle je nach Modus */}
      <TradesTable trades={isLive ? liveTrades : backtestTrades} />

      {/* Footer mit Modus-/KI-Hinweisen */}
      <AppFooter isLive={isLive} useMlVote={useMlVote} />
    </main>
  );
}
