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

## 🚀 3. Durchgeführte Arbeiten & Refactorings (Session 4)

### Architektur-Vertiefung & Dokumentation
*   **Wissens-Transfer**: Detaillierte Analyse und Dokumentation der Beziehung zwischen Frontend-Logik (JavaScript) und Backend-Logik (Java). 
*   **Dynamische Parameter-Übergabe**: Validierung des Workflows, bei dem das Frontend Konfigurationsobjekte (JSON) per POST an das Backend sendet. Das Backend (`HybridSignalEngine`) wertet diese Listen dynamisch aus und konfiguriert die Java-Indikatorklassen zur Laufzeit neu.

---

## 🧠 4. Wissensdatenbank & Erklärungen

### Warum nutzt das System React- UND Java-Logik?
Das System folgt einer **"Labor-zu-Produktion"-Philosophie**:
*   **Frontend (JavaScript) = Labor**: Hier findet das interaktive Experimentieren statt. Die lokale Berechnung erlaubt sofortiges visuelles Feedback im Replay-Modus und beim Verstellen der Regler, ohne Netzwerk-Latenz.
*   **Backend (Java) = Produktion**: Sobald eine Strategie im Labor validiert wurde, übernimmt Java den "schweren" Teil. Es führt die echten Trades 24/7 stabil auf dem Server aus und berechnet großflächige Backtests über tausende Kerzen performant.

### Dynamische Synchronisation
Die Indikatoren sind im Backend nicht mehr "hart" codiert. Das Frontend fungiert als Fernbedienung: Jede Änderung an einer SMA-Länge oder einem RSI-Schwellenwert im Browser wird sofort in ein JSON-Paket verpackt und an den Server geschickt. Java liest diese Werte dynamisch aus (via `IndicatorConfig`), sodass die "Produktion" immer exakt so läuft, wie sie im "Labor" eingestellt wurde.

### Mathematische Identität
Es ist von kritischer Bedeutung, dass die Formeln in `tradingviewindicators.js` (JS) und den Java-Klassen (z.B. `SmaIndicator.java`) exakt identisch sind. Nur so ist gewährleistet, dass ein im Replay gesehenes Signal später im Live-Handel exakt so ausgelöst wird (**"What You See Is What You Get"**).

---

## 🚀 4. Durchgeführte Arbeiten & Refactorings (Session 5)

### System-Stabilität & Fehlerbehebung
*   **Optionale ML-Integration**: Die `SimpleBacktestEngine` in Java wurde so optimiert, dass der ML-Service nur noch kontaktiert wird, wenn das `useMl`-Flag aktiv ist. Dies verhindert Timeouts und Abstürze bei reinen technischen Analysen (z.B. nur RSI).
*   **Robuste Signal-Engine**: Die `HybridSignalEngine` prüft nun explizit auf `anyIndicatorActive` und markiert unbekannte Indikatoren (wie MACD) als `UNSUPPORTED`, anstatt den Konsens zu verfälschen.
*   **React DOM-Fix**: Behebung eines `validateDOMNesting`-Fehlers in der `TradesTable.jsx`, der durch Kommentare innerhalb von Tabellen-Tags verursacht wurde.

### Intelligentes Status-Tracking
*   **Persistente Zeitstempel**: Der ML-Service (`api.py`) liest nun die Zeitstempel für das letzte Training und Daten-Update direkt aus den Metadaten der Dateien (`model.pkl` und `raw_1h.csv`). Dadurch bleiben die Informationen auch nach einem Server-Neustart im Dashboard erhalten.
*   **Absolute Pfad-Auflösung**: Umstellung auf `.resolve()` in Python, um Pfadprobleme beim Start aus unterschiedlichen Verzeichnissen zu vermeiden.

### UI/UX Optimierungen
*   **Preis-Kontext**: Der Zeitstempel der Marktdaten wurde direkt in die Preis-Karte integriert, um die Aktualität der Kurse sofort ersichtlich zu machen.
*   **Permanente Status-Anzeige**: Im ML-Panel werden die Zeitpunkte des letzten Trainings/Updates nun dauerhaft unter den Buttons angezeigt (statt nur nach einem Erfolg).

---
*Zuletzt aktualisiert: 19. Mai 2026*
