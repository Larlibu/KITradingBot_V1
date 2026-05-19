import React from 'react';

/**
 * React-Komponente für den Header der Anwendung.
 * Zeigt den Titel des Dashboards und den Modus-Umschalter an.
 * @param {object} props - Die Props der Komponente.
 * @param {boolean} props.isLive - Gibt an, ob der Live-Modus aktiv ist.
 * @param {Function} props.onViewModeChange - Callback-Funktion zum Ändern des Ansichtsmodus.
 * @returns {JSX.Element} Der gerenderte Header.
 */
export default function AppHeader({ isLive, onViewModeChange }) {
  return (
    <header className="main-header">
      <div>
        <h1>KI Trading Bot Dashboard</h1>
        <p>{isLive ? "Echtzeit-Überwachung" : "Strategie-Optimierung"}</p>
      </div>
      <div className="mode-switcher">
        <button
          className={isLive ? "mode-btn active live" : "mode-btn"}
          onClick={() => onViewModeChange("LIVE")}
        >
          Live-Modus
        </button>
        <button
          className={!isLive ? "mode-btn active analyse" : "mode-btn"}
          onClick={() => onViewModeChange("ANALYSE")}
        >
          Analyse-Modus
        </button>
      </div>
    </header>
  );
}
