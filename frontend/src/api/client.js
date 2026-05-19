import axios from "axios";

/**
 * Erstellt eine konfigurierte Axios-Instanz für die Kommunikation mit dem Backend.
 * Diese Instanz wird verwendet, um alle API-Anfragen an den Java Spring Boot Service zu senden.
 */
const api = axios.create({
  // Die Basis-URL des Backend-Services.
  // Wichtig: Hier wird der Port 8080 des Java-Backends verwendet,
  // um 404-Fehler zu vermeiden, die auftreten könnten, wenn das Frontend
  // versucht, direkt auf nicht existierende Endpunkte auf seinem eigenen Port (5173) zuzugreifen.
  baseURL: "http://localhost:8080/api",

  // Timeout für Anfragen in Millisekunden.
  // Erhöht auf 60 Sekunden, um genügend Zeit für längere Operationen wie
  // komplexe Backtests oder ML-Trainings-Trigger zu geben.
  timeout: 60000
});

export default api;
