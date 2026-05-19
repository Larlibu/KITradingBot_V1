/**
 * React-Komponente für das Konfigurationspanel der Indikatoren und der KI-Integration.
 * Ermöglicht das Hinzufügen, Entfernen und Anpassen von technischen Indikatoren
 * sowie das Aktivieren/Deaktivieren des KI-Konsens.
 * @param {object} props - Die Props der Komponente.
 * @param {Array<object>} props.indicators - Eine Liste der aktuell konfigurierten Indikatoren.
 * @param {Array<object>} props.templates - Eine Liste von Vorlagen für verfügbare Indikatoren.
 * @param {string} props.selectedType - Der aktuell im Dropdown ausgewählte Indikatortyp.
 * @param {Function} props.setSelectedType - Callback zum Aktualisieren des ausgewählten Indikatortyps.
 * @param {Function} props.onAdd - Callback zum Hinzufügen eines neuen Indikators.
 * @param {Function} props.onRemove - Callback zum Entfernen eines Indikators anhand seiner ID.
 * @param {Function} props.onParamChange - Callback zum Ändern eines Parameters eines Indikators.
 * @param {boolean} props.useMlVote - Gibt an, ob der KI-Konsens aktiviert ist.
 * @param {Function} props.setUseMlVote - Callback zum Umschalten des KI-Konsens.
 * @returns {JSX.Element} Das gerenderte Indikator-Konfigurationspanel.
 */
export default function IndicatorConfigPanel({
  indicators,
  templates,
  selectedType,
  setSelectedType,
  onAdd,
  onRemove,
  onParamChange,
  useMlVote,
  setUseMlVote
}) {
  return (
    <section className="panel config">
      <div className="panel-header-main">
        <h2>Konfiguration & Strategie</h2>
      </div>

      <div className="config-grid-layout">
        {/* Linke Seite: KI-Integrationseinstellungen */}
        <div className="config-card ki-card">
          <h3>KI-Integration</h3>
          <p className="card-description">Kombiniert technische Signale mit ML-Prognosen.</p>
          <label className="checkbox-label">
            <input
              type="checkbox"
              checked={useMlVote}
              onChange={(e) => setUseMlVote(e.target.checked)}
            />
            <span>KI-Konsens aktivieren</span>
          </label>
        </div>

        {/* Rechte Seite: Indikator-Auswahl und Hinzufügen */}
        <div className="config-card selection-card">
          <h3>Indikator hinzufügen</h3>
          <p className="card-description">Wähle einen Indikator für deine Strategie.</p>
          <div className="control-row no-margin">
            <select
              value={selectedType}
              onChange={(e) => setSelectedType(e.target.value)}
              aria-label="Indikatortyp auswählen"
            >
              {templates.map((t) => (
                <option key={t.type} value={t.type}>
                  {t.type}
                </option>
              ))}
            </select>
            <button className="primary-button" onClick={onAdd}>Hinzufügen</button>
          </div>
        </div>
      </div>

      {/* Bereich für aktive Indikatoren */}
      <div className="indicators-section">
        <h3>Aktive Indikatoren</h3>
        {indicators.length === 0 ? (
          <p className="empty-msg">Keine Indikatoren konfiguriert.</p>
        ) : (
          <div className="indicators-flex-container">
            {indicators.map((ind) => (
              <div key={ind.id} className="indicator-item-box">
                <div className="indicator-item-header">
                  <span className="indicator-name">{ind.type}</span>
                  <button
                    className="remove-btn"
                    onClick={() => onRemove(ind.id)}
                    title="Indikator entfernen"
                    aria-label={`Entferne ${ind.type}`}
                  >
                    &times;
                  </button>
                </div>
                <div className="indicator-params-grid">
                  {Object.entries(ind.params).map(([key, val]) => (
                    <div key={key} className="param-field">
                      <label htmlFor={`${ind.id}-${key}`}>{key}</label>
                      <input
                        id={`${ind.id}-${key}`}
                        type="number"
                        value={val}
                        onChange={(e) => onParamChange(ind.id, key, e.target.value)}
                        aria-label={`${ind.type} Parameter ${key}`}
                      />
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </section>
  );
}
