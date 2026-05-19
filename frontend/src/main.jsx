import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";
import "./styles.css";

/**
 * Der Einstiegspunkt der React-Anwendung.
 * Rendert die Hauptkomponente <App /> in das DOM-Element mit der ID 'root'.
 */
ReactDOM.createRoot(document.getElementById("root")).render(
  // React.StrictMode aktiviert zusätzliche Prüfungen und Warnungen für React-Komponenten
  // während der Entwicklung, um potenzielle Probleme frühzeitig zu erkennen.
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
