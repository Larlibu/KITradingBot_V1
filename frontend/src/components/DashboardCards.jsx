/**
 * React-Komponente zur Anzeige der wichtigsten Dashboard-Karten.
 * Zeigt den aktuellen Preis, das Handelssignal, die KI-Konfidenz
 * und dynamisch alle konfigurierten Indikatorwerte an.
 * @param {object} props - Die Props der Komponente.
 * @param {number} props.price - Der aktuell anzuzeigende Preis.
 * @param {string} props.signal - Das aktuell anzuzeigende Handelssignal (BUY, SELL, HOLD).
 * @param {number} props.confidence - Die Konfidenz der KI-Vorhersage (0.0 bis 1.0).
 * @param {Array<object>} [props.indicators=[]] - Eine Liste von Objekten { name, value } für die anzuzeigenden Indikatoren.
 * @param {object} props.serverStatus - Der Status der Backend-Dienste (backend, mlService).
 * @param {string} props.viewMode - Der aktuelle Ansichtsmodus ('LIVE' oder 'ANALYSE').
 * @returns {JSX.Element} Das gerenderte Dashboard mit Karten.
 */
export default function DashboardCards({ price, signal, confidence, indicators = [], serverStatus, viewMode }) {
  const isLive = viewMode === "LIVE";

  // Formatiert das Signal für die Anzeige
  const signalLabel = signal === "BUY" ? "KAUFEN (LONG)" : signal === "SELL" ? "VERKAUFEN (SHORT)" : "WARTEN (HOLD)";
  // Bestimmt die Farbe des Signals
  const signalColor = signal === "BUY" ? "var(--success)" : signal === "SELL" ? "var(--danger)" : "var(--text-main)";

  /**
   * Hilfskomponente zur Anzeige des Status eines Dienstes (Online/Offline).
   * @param {object} props - Die Props der Komponente.
   * @param {boolean} props.online - Gibt an, ob der Dienst online ist.
   * @param {string} props.label - Der Name des Dienstes.
   * @returns {JSX.Element} Ein Status-Badge.
   */
  const StatusBadge = ({ online, label }) => (
    <div style={{ display: 'flex', alignItems: 'center', gap: '5px', fontSize: '0.75rem' }}>
      <div style={{
        width: '8px',
        height: '8px',
        borderRadius: '50%',
        backgroundColor: online ? '#4CAF50' : '#F44336' // Grün für Online, Rot für Offline
      }} />
      <span>{label}</span>
    </div>
  );

  return (
    <div className="dashboard-container">
      {/* Obere Statusleiste mit Modus und Dienststatus */}
      <div className="top-status-bar" style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        padding: '10px 15px',
        marginBottom: '15px',
        background: isLive ? 'rgba(37, 99, 235, 0.1)' : 'rgba(245, 158, 11, 0.1)', // Modus-spezifischer Hintergrund
        border: `1px solid ${isLive ? 'rgba(37, 99, 235, 0.3)' : 'rgba(245, 158, 11, 0.3)'}`, // Modus-spezifischer Rahmen
        borderRadius: '8px'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '15px' }}>
          {/* Modus-Anzeige */}
          <div style={{
            padding: '4px 12px',
            borderRadius: '20px',
            backgroundColor: isLive ? '#2563eb' : '#f59e0b',
            color: 'white',
            fontWeight: 'bold',
            fontSize: '0.8rem',
            textTransform: 'uppercase',
            letterSpacing: '1px'
          }}>
            {isLive ? '● Live-Modus' : '🔬 Analyse-Modus'}
          </div>
          <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
            {isLive ? 'Echtzeit-Daten vom Server' : 'Historische Daten & Simulation'}
          </span>
        </div>

        {/* Status-Badges für Backend und ML-Dienst */}
        <div style={{ display: 'flex', gap: '20px' }}>
          <StatusBadge online={serverStatus?.backend} label="Backend" />
          <StatusBadge online={serverStatus?.mlService} label="ML-Dienst" />
        </div>
      </div>

      {/* Karten-Grid für Preis, Signal, Konfidenz und Indikatoren */}
      <div className="cards" style={{
          display: 'grid',
          // Dynamische Spaltenanzahl: 3 feste Karten + Anzahl der aktiven Indikatoren
          gridTemplateColumns: `repeat(${3 + indicators.length}, 1fr)`,
          gap: '15px'
      }}>
        {/* Karte: Aktueller Preis */}
        <div className="card">
          <p className="card-label">Preis</p>
          <h3 style={{ color: isLive ? 'inherit' : '#f59e0b' }}>${price.toFixed(2)}</h3>
        </div>

        {/* Karte: Handelssignal */}
        <div className="card">
          <p className="card-label">Signal</p>
          <h3 style={{ color: signalColor }}>{signalLabel}</h3>
        </div>

        {/* Karte: KI-Konfidenz */}
        <div className="card">
          <p className="card-label">KI-Konfidenz</p>
          <h3>{(confidence * 100).toFixed(1)}%</h3>
        </div>

        {/* Dynamische Karten für jeden aktiven Indikator */}
        {indicators.map((ind, i) => (
          <div className="card" key={i}>
            <p className="card-label">{ind.name}</p>
            <h3 style={{ fontSize: '1.2rem' }}>{ind.value}</h3>
          </div>
        ))}
      </div>
    </div>
  );
}
