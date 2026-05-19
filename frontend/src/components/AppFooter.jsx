import React from 'react';

/**
 * React-Komponente für den Footer der Anwendung.
 * Zeigt den aktuellen Modus und den Status des KI-Konsens an.
 * @param {object} props - Die Props der Komponente.
 * @param {boolean} props.isLive - Gibt an, ob der Live-Modus aktiv ist.
 * @param {boolean} props.useMlVote - Gibt an, ob der KI-Konsens aktiviert ist.
 * @returns {JSX.Element} Der gerenderte Footer.
 */
export default function AppFooter({ isLive, useMlVote }) {
  return (
    <footer>
      Status: {isLive ? "Echtzeit-Polling" : "Analyse"} | Konsens: {useMlVote ? "KI aktiv" : "Nur Technik"}
    </footer>
  );
}
