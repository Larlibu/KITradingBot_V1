# 📝 Projekt-Log & Wissensdatenbank: KITradingBot (Session 2 & 3)

Dieses Dokument fasst die Phasen der Entwicklung zusammen, in der der Fokus auf UI/UX-Optimierung, Systemstabilität und Datenautomatisierung lag.

---

## 🛠 1. Durchgeführte Arbeiten & Refactorings (Session 2)

### Architektur & Clean Code (Frontend)
*   **Custom Hooks Integration**: Die gesamte Geschäftslogik wurde aus der `App.jsx` in spezialisierte Hooks ausgelagert (`useTrading`, `useBacktesting`, `useReplay`, `useIndicators`). Die `App.jsx` dient nun nur noch als schlanke View-Komponente (MVC-Prinzip).
*   **Zwei-Spalten-Layout**: Umstellung auf ein professionelles Dashboard-Design mit einer Sidebar für Steuerung/Konfiguration (links) und einem großen Chart-Bereich (rechts).
*   **Modus-Management**: Einführung eines "Live-Modus" (Überwachung) und eines "Analyse-Modus" (Strategie-Entwicklung) mit klarer farblicher Trennung (Blau vs. Bernstein).

### Daten-Automatisierung (Python & Java)
*   **Binance Downloader**: Integration eines Python-Skripts zum automatischen Abruf der letzten 2 Jahre Marktdaten von der Binance API.
*   **Echtzeit-Daten-Brücke**: Das Java-Backend unterscheidet nun intelligent zwischen Live-Anfragen (direkt von Binance API) und Backtest-Anfragen (vom ML-Service aus lokalen CSV-Daten).
*   **Automatisches Feature Engineering**: Der ML-Service berechnet nun beim Download direkt die benötigten Indikatoren (SMA, RSI) und generiert die Ziel-Labels (`STEIGT`/`FÄLLT`).

### Machine Learning (ML)
*   **Status-Tracking**: Implementierung eines asynchronen Status-Systems. Das Dashboard zeigt nun in Echtzeit an, ob die KI gerade trainiert oder Daten lädt.
*   **Lokalisierung**: Umstellung aller internen KI-Labels von `UP/DOWN` auf `STEIGT/FÄLLT` für eine konsistente deutsche Benutzerführung.
*   **Robuste API**: Korrektur von Pfadproblemen und Definitionen (`NameError: PredictRequest`) im Python-Dienst.

---

## 🚀 2. Durchgeführte Arbeiten & Refactorings (Session 3)

### Performance & ML-Integration
*   **Batch-Prediction**: Einführung eines `/predict/batch` Endpunkts im Python ML-Service. Das Java-Backend sendet nun alle Backtest-Features in einem einzigen Call. Dies reduzierte die Backtest-Dauer von ~40s auf < 1s (Behebung des Axios-Timeouts).
*   **Dynamische Signal-Engine**: Refactoring der `HybridSignalEngine` in Java, um das `useMl`-Flag vom Frontend zu berücksichtigen. Die KI ist nun optionaler Bestandteil des Konsens.

### Strategie-Synchronisation (Frontend ↔ Backend)
*   **Indicator Transfer**: Implementierung einer dynamischen Synchronisation. Die kompletten Indikator-Einstellungen aus React (Periode, Schwellenwerte) werden nun bei jedem Backtest- oder Signal-Aufruf per POST-Request an das Java-Backend übertragen.
*   **Universal Indicator Interface**: Die Java-Indikatoren (`SMA`, `RSI`) wurden so umgebaut, dass sie ihre Parameter dynamisch zur Laufzeit empfangen können.

### Analyse-Modus & Replay-System
*   **Visuelles Replay 2.0**: Implementierung einer automatischen Abspielfunktion mit Geschwindigkeitssteuerung.
*   **Smart-Stop-Automatik**: Das Replay hält nun bei jedem `BUY`- oder `SELL`-Signal automatisch an und zeigt ein modales Fenster mit der Begründung an.
*   **Echtzeit-Dashboard im Replay**: Die Dashboard-Werte (Preis, SMA, RSI, KI-Konfidenz) aktualisieren sich nun synchron zu jeder Kerze während des Abspielens.

### UI/UX & Fehlerbehebungen
*   **DASHBOARD Dynamik**: Die Dashboard-Karten passen sich nun automatisch an die Anzahl der konfigurierten Indikatoren an (Horizontal-Layout).
*   **Initial-Load Fix**: Marktdaten werden nun sofort beim Mounten der App geladen, wodurch das "manuelle Umschalten" zum ersten Anzeigen der Kerzen entfällt.

---

## 🧠 3. Wissensdatenbank & Erklärungen

### Warum nutzt das System React- UND Java-Logik?
React (Frontend) berechnet die Indikatoren für das visuelle Replay lokal im Browser, um sofortige Interaktion ohne Netzwerk-Verzögerung zu ermöglichen. Java (Backend) übernimmt die performante Massenberechnung (Backtest) und den stabilen Live-Handel. Durch die Synchronisation nutzen beide nun identische Parameter ("What you see is what you get").

### Warum wurde von GET auf POST umgestellt?
Um die kompletten Indikator-Listen (Arrays mit Objekten und Parametern) vom Browser an den Server zu senden, ist das POST-Verfahren sicherer und flexibler als die Limitierung durch URL-Parameter (GET).

### Die Rolle der Batch-Vorhersage
Netzwerk-Overhead ist bei vielen kleinen Anfragen der größte Zeitfresser. Durch das Bündeln aller 500 Kerzen in eine Anfrage wird die KI-Analyse massiv beschleunigt und System-Timeouts werden vermieden.

---
*Zuletzt aktualisiert: 18. November 2024*
