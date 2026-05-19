export default function BacktestPanel({ backtest, onRun, isLoading }) {
  return (
    <section className="panel backtest" style={{ position: 'relative' }}>
      <h2>Strategie-Backtest</h2>
      <p className="card-description">Simulation auf Basis der letzten 500 Stunden.</p>

      <div className="control-row">
        <button
          className="primary-button"
          onClick={onRun}
          disabled={isLoading}
          style={{
            width: '100%',
            backgroundColor: isLoading ? '#334155' : 'var(--warning)',
            color: 'white',
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            gap: '10px'
          }}
        >
          {isLoading ? (
            <>
              <span className="loader-dots">Berechne...</span>
            </>
          ) : "Backtest starten"}
        </button>
      </div>

      {isLoading && (
        <div style={{ marginTop: '15px', textAlign: 'center' }}>
          <p style={{ color: '#fbbf24', fontSize: '0.85rem' }}>⏳ Strategie wird simuliert...</p>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.7rem' }}>(Dies kann bei aktiver KI bis zu 30s dauern)</p>
        </div>
      )}

      {backtest && !isLoading && (
        <div className="backtest-results" style={{ marginTop: '20px', borderTop: '1px solid var(--border-color)', paddingTop: '15px' }}>
          <div className="result-grid">
            <div className="result-item">
              <p className="card-label">Rendite</p>
              <strong style={{ color: backtest.strategyReturnPct >= 0 ? "#4ade80" : "#f87171", fontSize: '1.2rem' }}>
                {backtest.strategyReturnPct.toFixed(2)}%
              </strong>
            </div>
            <div className="result-item">
              <p className="card-label">Trades</p>
              <strong>{backtest.totalTrades}</strong>
            </div>
            <div className="result-item">
              <p className="card-label">Win-Rate</p>
              <strong>{backtest.winRate.toFixed(1)}%</strong>
            </div>
          </div>
          <div style={{ marginTop: '10px', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
            Vergleich: Buy & Hold {backtest.buyHoldReturnPct.toFixed(2)}%
          </div>
        </div>
      )}
    </section>
  );
}
