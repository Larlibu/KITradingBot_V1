import React from 'react';

/**
 * React-Komponente für das modale Overlay, das bei einem Handelssignal im Replay-Modus erscheint.
 * @param {object} props - Die Props der Komponente.
 * @param {object|null} props.signal - Das Handelssignal-Objekt, das das Modal auslösen soll.
 * @param {Function} props.onContinue - Callback-Funktion, die aufgerufen wird, wenn der Benutzer auf "Weiter analysieren" klickt.
 * @returns {JSX.Element|null} Das modale Overlay oder null, wenn kein Signal vorhanden ist.
 */
export default function TradeSignalModal({ signal, onContinue }) {
  if (!signal) {
    return null;
  }

  return (
    <div className="modal-overlay">
      <div className="modal-content trade-alert">
        <h3>🚨 Signal erkannt!</h3>
        <div className={`signal-badge signal-${signal.type.toLowerCase()}`}>
          {signal.type} bei {signal.price.toFixed(2)}
        </div>
        <p className="reason">{signal.reason}</p>
        <button className="primary-button" onClick={onContinue}>Weiter analysieren</button>
      </div>
    </div>
  );
}
