/**
 * React-Komponente für das Kontrollpanel des Trading-Bots im Live-Modus.
 * Ermöglicht das Starten und Stoppen des automatisierten Handels
 * sowie die Konfiguration des KI-Konsens.
 * @param {object} props - Die Props der Komponente.
 * @param {boolean} props.tradingEnabled - Gibt an, ob der automatisierte Handel aktiv ist.
 * @param {Function} props.onStart - Callback-Funktion zum Starten des Handels.
 * @param {Function} props.onStop - Callback-Funktion zum Stoppen des Handels.
 * @param {boolean} props.useMlVote - Gibt an, ob der KI-Konsens aktiviert ist.
 * @param {Function} props.setUseMlVote - Callback-Funktion zum Umschalten des KI-Konsens.
 * @returns {JSX.Element} Das gerenderte Kontrollpanel.
 */
export default function ControlsPanel({
  tradingEnabled,
  onStart,
  onStop,
  useMlVote,
  setUseMlVote
}) {
  return (
    <section className="panel controls">
      <h2>Bot-Steuerung (Live)</h2>

      {/* Schalter für den KI-Konsens */}
      <div style={{
        marginBottom: '20px',
        padding: '12px',
        background: 'rgba(255,255,255,0.03)',
        borderRadius: '8px',
        border: '1px solid var(--border-color)'
      }}>
        <label className="checkbox-label" style={{ margin: 0 }}>
          <input
            type="checkbox"
            checked={useMlVote}
            onChange={(e) => setUseMlVote(e.target.checked)}
          />
          <span style={{ fontWeight: 'bold', fontSize: '0.9rem' }}>KI-Konsens erzwingen</span>
        </label>
        <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '5px' }}>
          Nur handeln, wenn KI und Technik übereinstimmen.
        </p>
      </div>

      {/* Buttons zum Starten und Stoppen des Handels */}
      <div className="control-row">
        <button
          className="primary-button"
          onClick={onStart}
          disabled={tradingEnabled} // Deaktiviert, wenn der Handel bereits läuft
          style={{ flex: 1, backgroundColor: tradingEnabled ? '#334155' : '#22c55e' }}
        >
          {tradingEnabled ? "Handel läuft..." : "Trading starten"}
        </button>
        <button
          className="secondary"
          onClick={onStop}
          disabled={!tradingEnabled} // Deaktiviert, wenn der Handel nicht läuft
          style={{ flex: 1 }}
        >
          Stop
        </button>
      </div>
    </section>
  );
}
