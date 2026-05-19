# 📝 Projekt-Log & Wissensdatenbank: KITradingBot

Dieses Dokument enthält eine Zusammenfassung der durchgeführten Arbeiten sowie detaillierte Erklärungen zu den Kernkonzepten des Projekts.

---

## 🛠 1. Durchgeführte Arbeiten & Refactorings

### Backend (Java)
*   **Vollständige Dokumentation**: Alle Klassen (Controller, Services, Models, Repositories, Indikatoren) wurden mit detaillierten deutschen Javadoc-Kommentaren versehen.
*   **Clean Code Refactoring**: Die `SimpleBacktestEngine` wurde optimiert. Die komplexe Logik innerhalb der Schleife wurde in die Methode `processTick()` sowie in spezifische `executeBuy()`/`executeSell()` Methoden ausgelagert.
*   **ML-Training Endpunkt**: Der `PredictionController` und `PredictionService` wurden um die Möglichkeit erweitert, das Modell-Training manuell via REST anzustoßen.
*   **CORS-Anpassung**: Die `WebConfig` wurde so erweitert, dass alle Pfade (inkl. Actuator) für das Frontend erreichbar sind.
*   **Javadoc-Vorbereitung**: Die `pom.xml` wurde für die korrekte Generierung der Dokumentation (UTF-8 Support) konfiguriert.

### ML-Service (Python/FastAPI)
*   **API-Erweiterung**: Ein neuer Endpunkt `/train` wurde in `api.py` hinzugefügt, der den Trainingsprozess aus `train.py` als Hintergrundtask startet.
*   **CORS-Support**: Die `CORSMiddleware` wurde hinzugefügt, um Anfragen vom Frontend zu erlauben.
*   **Lokalisierung**: Die Labels der KI-Vorhersage wurden von `UP`/`DOWN` auf `STEIGT`/`FÄLLT` umgestellt.

### Frontend (React)
*   **Vollständige Lokalisierung**: Die gesamte Benutzeroberfläche wurde ins Deutsche übersetzt.
*   **Server-Status-Anzeige**: Ein neues Panel im Dashboard zeigt in Echtzeit (Polling alle 10s), ob das Backend und der ML-Service online sind.
*   **ML-Trainings-Button**: Ein neuer Button in den "Controls" ermöglicht das Starten des Modell-Trainings direkt aus dem Browser.
*   **UI-Fixes**: Das Layout der Indikator-Konfiguration wurde in ein sauberes Grid-Design (Kacheln nebeneinander) umgewandelt.

### System-Skripte
*   **Start-Skript (`start-all.ps1`)**: Optimiert durch längere Timeouts (90s für Java) und stabilere Aufrufe via `cmd /c`.
*   **Stop-Skript (`stop-all.ps1`)**: Umgeschrieben auf eine portbasierte Suche. Es beendet nun zuverlässig alle Prozesse, die die Ports 8000, 8080 und 5173 belegen (Lösung von "Zombie-Prozessen").

---

## 🧠 2. Wissensdatenbank & Konzepte

### Warum läuft das Backtesting im Frontend?
Das Backtesting läuft bewusst frontend-seitig mit den aktuell konfigurierten Indikatoren. Dies ermöglicht ein **"What You See Is What You Get" (WYSIWYG)** Prinzip: Die Engine im Browser nutzt exakt dieselben Parameter, die der Nutzer im Chart sieht. Dies bietet maximale Flexibilität beim Experimentieren mit Strategien, ohne dass Backend-Code geändert werden muss.

### Was ist ein "Feature"?
Ein Feature ist ein aufbereitetes Merkmal aus den Rohdaten. Die KI sieht keine Charts, sondern nur Zahlenwerte. In diesem Projekt besteht der "Feature-Vektor" aus:
1.  **Schlusskurs**: Wo steht der Preis?
2.  **SMA**: Was ist der Trend-Durchschnitt?
3.  **RSI**: Ist der Markt überhitzt?
4.  **Differenz (Preis - SMA)**: Wie weit ist der Preis vom Durchschnitt entfernt?

### KI-Vorhersage: STEIGT, FÄLLT & Konfidenz
*   **Label (STEIGT/FÄLLT)**: Die Richtung, die das Modell für die nächste Kerze prognostiziert.
*   **Konfidenz**: Die statistische Sicherheit der KI (z.B. 85%).
*   **KI-Konsens**: Wenn aktiviert, wird ein Trade nur ausgeführt, wenn technische Indikatoren (RSI/SMA) **und** die KI (mit hoher Sicherheit) in dieselbe Richtung zeigen.

### Java vs. JavaScript Indikatoren
*   **JavaScript-Indikatoren** (`tradingViewIndicators.js`): Werden für die UI, den Replay-Modus und den Frontend-Backtest genutzt. Sie ermöglichen schnelles Feedback im Dashboard.
*   **Java-Indikatoren** (`IndicatorFeaturePipeline.java`): Werden für den Live-Betrieb des Bots im Backend genutzt. Sie müssen mathematisch identisch zu den JS-Indikatoren sein, damit die KI im Live-Betrieb die gleichen Muster erkennt wie im Backtest.

---
*Dokumentation erstellt am: 16. Mai 2024*
