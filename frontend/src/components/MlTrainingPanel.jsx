import { useState, useEffect, useRef } from "react";
import { triggerMlTraining, triggerDataUpdate } from "../api/tradingApi";
import api from "../api/client"; // Direkter Axios-Client für Statusabfragen

/**
 * React-Komponente für das Panel zur Optimierung des KI-Modells.
 * Ermöglicht das Auslösen des Marktdaten-Downloads und des KI-Modelltrainings
 * sowie die Anzeige des aktuellen Status dieser asynchronen Operationen.
 * @returns {JSX.Element} Das gerenderte ML-Trainingspanel.
 */
export default function MlTrainingPanel() {
  // --- Zustandsvariablen für das Modelltraining ---
  /** @type {[string, Function]} Der aktuelle Status des Modelltrainings ('idle', 'running', 'success', 'error'). */
  const [trainingStatus, setTrainingStatus] = useState("idle");
  /** @type {[string|null, Function]} Zeitstempel des letzten erfolgreichen Trainingslaufs. */
  const [lastTrainingRun, setLastTrainingRun] = useState(null);
  /** @type {[string|null, Function]} Fehlermeldung, falls das Training fehlschlägt. */
  const [trainingErrorMessage, setTrainingErrorMessage] = useState(null);

  // --- Zustandsvariablen für das Daten-Update ---
  /** @type {[string, Function]} Der aktuelle Status des Daten-Updates ('idle', 'running', 'success', 'error'). */
  const [dataUpdateStatus, setDataUpdateStatus] = useState("idle");
  /** @type {[string|null, Function]} Zeitstempel des letzten erfolgreichen Daten-Updates. */
  const [lastDataUpdate, setLastDataUpdate] = useState(null);
  /** @type {[string|null, Function]} Fehlermeldung, falls das Daten-Update fehlschlägt. */
  const [dataUpdateErrorMessage, setDataUpdateErrorMessage] = useState(null);

  // Referenzen für Timer, um Polling-Intervalle zu verwalten und zu bereinigen.
  const trainingTimerRef = useRef(null);
  const dataUpdateTimerRef = useRef(null);

  // --- Funktionen zum Abrufen des Status ---

  /**
   * Ruft den aktuellen Status des Modelltrainings vom Backend ab.
   * Startet bei Bedarf ein erneutes Polling, wenn das Training noch läuft.
   */
  const fetchTrainingStatus = async () => {
    try {
      const res = await api.get("/predict/train/status");
      setTrainingStatus(res.data.status);
      setLastTrainingRun(res.data.last_run);
      setTrainingErrorMessage(res.data.error_message);

      // Wenn das Training noch läuft, setze einen neuen Timer für das nächste Polling.
      if (res.data.status === "running") {
        trainingTimerRef.current = setTimeout(fetchTrainingStatus, 2000);
      }
    } catch (e) {
      console.error("Fehler beim Abrufen des Trainingsstatus:", e);
      setTrainingStatus("error");
      setTrainingErrorMessage("Status-Abfrage fehlgeschlagen.");
    }
  };

  /**
   * Ruft den aktuellen Status des Marktdaten-Updates vom Backend ab.
   * Startet bei Bedarf ein erneutes Polling, wenn das Update noch läuft.
   */
  const fetchDataUpdateStatus = async () => {
    try {
      const res = await api.get("/predict/data/status");
      setDataUpdateStatus(res.data.status);
      setLastDataUpdate(res.data.last_update);
      setDataUpdateErrorMessage(res.data.error_message);

      // Wenn das Daten-Update noch läuft, setze einen neuen Timer für das nächste Polling.
      if (res.data.status === "running") {
        dataUpdateTimerRef.current = setTimeout(fetchDataUpdateStatus, 2000);
      }
    } catch (e) {
      console.error("Fehler beim Abrufen des Daten-Update-Status:", e);
      setDataUpdateStatus("error");
      setDataUpdateErrorMessage("Status-Abfrage fehlgeschlagen.");
    }
  };

  // --- useEffect Hook für initiales Laden und Cleanup ---
  useEffect(() => {
    // Ruft den Status beim ersten Rendern der Komponente ab.
    fetchTrainingStatus();
    fetchDataUpdateStatus();

    // Cleanup-Funktion: Stoppt alle Polling-Timer, wenn die Komponente unmounted wird.
    return () => {
      clearTimeout(trainingTimerRef.current);
      clearTimeout(dataUpdateTimerRef.current);
    };
  }, []); // Leeres Abhängigkeits-Array bedeutet, dass der Effekt nur einmal beim Mounten läuft.

  // --- Event-Handler ---

  /**
   * Behandelt den Klick auf den "KI jetzt trainieren"-Button.
   * Setzt den Status auf 'running' und löst das Training im Backend aus.
   */
  const handleTrainClick = async () => {
    setTrainingStatus("running");
    setTrainingErrorMessage(null);
    try {
      await triggerMlTraining();
      // Starte das Polling kurz nach dem Auslösen, um den Status zu aktualisieren.
      setTimeout(fetchTrainingStatus, 500);
    } catch (error) {
      setTrainingStatus("error");
      // Extrahiere die Fehlermeldung aus der Axios-Antwort oder nutze einen Fallback.
      const msg = error.response?.data?.message || "ML-Service nicht erreichbar.";
      setTrainingErrorMessage(msg);
    }
  };

  /**
   * Behandelt den Klick auf den "Marktdaten aktualisieren"-Button.
   * Setzt den Status auf 'running' und löst das Daten-Update im Backend aus.
   */
  const handleDataUpdateClick = async () => {
    setDataUpdateStatus("running");
    setDataUpdateErrorMessage(null);
    try {
      await triggerDataUpdate();
      // Starte das Polling kurz nach dem Auslösen, um den Status zu aktualisieren.
      setTimeout(fetchDataUpdateStatus, 500);
    } catch (error) {
      setDataUpdateStatus("error");
      const msg = error.response?.data?.message || "ML-Service nicht erreichbar.";
      setDataUpdateErrorMessage(msg);
    }
  };

  // --- JSX Rendering ---
  return (
    <section className="panel ml-training">
      <h3>KI-Modell Optimierung</h3>
      <p className="card-description">Marktdaten aktualisieren und KI-Modell neu trainieren.</p>

      {/* Daten-Update Sektion */}
      <div style={{ marginBottom: '20px', paddingBottom: '15px', borderBottom: '1px solid var(--border-color)' }}>
        <button
          className="primary-button"
          onClick={handleDataUpdateClick}
          disabled={dataUpdateStatus === "running"} // Button deaktivieren, wenn Update läuft
          style={{
              width: '100%',
              backgroundColor: dataUpdateStatus === "running" ? '#334155' : '#2563eb',
              color: 'white'
          }}
        >
          {dataUpdateStatus === "running" ? "Daten werden geladen..." : "Marktdaten aktualisieren"}
        </button>
        <div style={{ marginTop: '10px' }}>
          {dataUpdateStatus === "running" && <p style={{ color: '#fbbf24', fontSize: '0.85rem' }}>⏳ Lade Daten von Binance...</p>}
          {dataUpdateStatus === "success" && <p style={{ color: '#4ade80', fontSize: '0.85rem' }}>✅ Daten aktuell! ({lastDataUpdate})</p>}
          {dataUpdateStatus === "error" && (
              <div style={{ color: '#f87171', fontSize: '0.85rem' }}>
                  <p>❌ Fehler beim Daten-Update:</p>
                  <p style={{ opacity: 0.8 }}>{dataUpdateErrorMessage}</p>
              </div>
          )}
        </div>
      </div>

      {/* Training Sektion */}
      <div>
        <button
          className="primary-button"
          onClick={handleTrainClick}
          disabled={trainingStatus === "running"} // Button deaktivieren, wenn Training läuft
          style={{
              width: '100%',
              backgroundColor: trainingStatus === "running" ? '#334155' : '#f59e0b',
              color: 'white'
          }}
        >
          {trainingStatus === "running" ? "KI lernt gerade..." : "KI jetzt trainieren"}
        </button>
        <div style={{ marginTop: '10px' }}>
          {trainingStatus === "running" && <p style={{ color: '#fbbf24', fontSize: '0.85rem' }}>⏳ KI lernt aus historischen Daten...</p>}
          {trainingStatus === "success" && <p style={{ color: '#4ade80', fontSize: '0.85rem' }}>✅ Modell ist auf dem neuesten Stand! ({lastTrainingRun})</p>}
          {trainingStatus === "error" && (
              <div style={{ color: '#f87171', fontSize: '0.85rem' }}>
                  <p>❌ Fehler beim Training:</p>
                  <p style={{ opacity: 0.8 }}>{trainingErrorMessage}</p>
              </div>
          )}
        </div>
      </div>
    </section>
  );
}
