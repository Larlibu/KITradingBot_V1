# 📝 Projekt-Log & Wissensdatenbank: KITradingBot (Session 2024-07-30)

Dieses Dokument fasst die heutigen Arbeiten, Refactorings und Problembehebungen zusammen.

---

## 🛠 1. Durchgeführte Arbeiten & Refactorings

### 1.1 Behebung: Frontend "Verbindung abgelehnt"
*   **Problem:** Das Frontend war nach Änderungen manchmal nicht erreichbar, da alte Prozesse Ports blockierten.
*   **Lösung:** Das `start-all.ps1` Skript wurde modifiziert, um vor dem Start aller Dienste automatisch das `stop-all.ps1` Skript auszuführen. Dies gewährleistet einen sauberen Neustart und vermeidet Port-Konflikte.

### 1.2 Behebung: Indikatorwerte im Dashboard verschwanden/fehlten
*   **Problem:** Beim Wechsel in den Replay-Modus oder beim Starten des Replays verschwanden die Indikatorwerte im Dashboard oder zeigten nur die KI-Konfidenz an.
*   **Ursache:**
    *   `replaySignal` war initial `null` oder nicht vollständig mit Indikator-Votes befüllt.
    *   Die Dashboard-Logik in `App.jsx` konnte die Werte nicht korrekt aus den `signalSnapshot.features` extrahieren oder die Filterung war zu streng.
    *   Die `IndicatorFeaturePipeline` im Backend nutzte noch statische Indikatorparameter, was zu Fehlern führte, wenn die Java-Indikatoren dynamisiert wurden.
*   **Lösung:**
    *   **`useReplay.js`:** Stellt sicher, dass `replaySignal` sofort beim Aktivieren des Replays mit den Analyseergebnissen der aktuellen Kerze befüllt wird.
    *   **`useBacktesting.js`:** Stellt sicher, dass die `votes`-Liste (Indikator-Ergebnisse) immer korrekt im `replaySignal` zurückgegeben wird.
    *   **`App.jsx`:** Die Logik zur Extraktion der `activeIndicatorValues` wurde robuster gestaltet, um Werte sowohl aus `replaySignal.votes` (Replay) als auch aus `liveConsensus.votes` oder `signalSnapshot.features` (Live) korrekt zu parsen und anzuzeigen. Fallbacks ("...") wurden für fehlende Daten implementiert.
    *   **`FeaturePipeline.java` & `IndicatorFeaturePipeline.java`:** Die Feature-Pipeline im Backend wurde dynamisiert, um die `IndicatorConfig`-Liste vom Frontend zu akzeptieren und die Indikatoren mit den korrekten Parametern zu berechnen.

### 1.3 Dynamische Indikator-Anzeige im Chart
*   **Problem:** Nur eine statische SMA-Linie wurde im Chart angezeigt; andere konfigurierte Indikatoren (EMA, RSI, BBANDS) waren nicht sichtbar.
*   **Lösung:**
    *   **`CandlesChart.jsx`:** Wurde vollständig dynamisiert. Es akzeptiert nun die `indicators`-Liste vom Frontend.
    *   **Overlay-Indikatoren:** Für jeden konfigurierten SMA, EMA und Bollinger Band wird nun eine eigene Linie auf der rechten Preisachse gezeichnet.
    *   **Oszillatoren:** Der RSI wird auf einer separaten linken Preisachse gezeichnet, um die Skalierungsprobleme mit dem Preis zu vermeiden. Horizontale Linien für Overbought/Oversold wurden hinzugefügt.
    *   **Berechnungsfunktionen:** Hilfsfunktionen (`calculateEma`, `calculateBbands`, `calculateRsi`) wurden in `CandlesChart.jsx` implementiert, um die Indikatoren im Frontend zu berechnen.

### 1.4 Behebung: Keine Signale im Backend
*   **Problem:** Trotz korrekter Indikatorkonfiguration wurden keine Kauf-/Verkaufssignale generiert.
*   **Ursache:**
    *   Die `HybridSignalEngine` im Backend unterstützte nur SMA und RSI für die Signalgenerierung. Neu hinzugefügte Indikatoren (EMA, BBANDS) wurden ignoriert.
    *   Die Konsens-Logik war sehr streng (alle Indikatoren mussten übereinstimmen).
*   **Lösung:**
    *   **Neue Java-Indikatoren:** `EmaIndicator.java` und `BbandsIndicator.java` wurden im Backend erstellt und in die `HybridSignalEngine` integriert.
    *   **Erweiterte `HybridSignalEngine`:** Die Engine wurde angepasst, um alle dynamisch übergebenen Indikatoren (SMA, EMA, RSI, BBANDS) in die Konsens-Logik einzubeziehen.
    *   **Transparentere Begründung:** Die `reason` im `SignalDecision` wurde verbessert, um genau anzuzeigen, welche Indikatoren welche Signale geben, wenn kein Konsens erreicht wird (z.B. "Kein Konsens: KI=BUY, SMA=SELL").

### 1.5 Git-Vorbereitung
*   **Lösung:** Eine umfassende `.gitignore`-Datei wurde im Projekt-Root erstellt, um Build-Artefakte, IDE-Einstellungen, Node-Module, Python-Environments, ML-Modelle und Rohdaten vom Versionskontrollsystem auszuschließen.

---

## 🧠 2. Wissensdatenbank & Erklärungen

### Q: Warum wurden die Indikatoren im Chart nicht angezeigt?
*   **A:** Das Chart war ursprünglich nur für eine statische SMA-Linie konfiguriert. Um alle vom Benutzer ausgewählten Indikatoren anzuzeigen, musste das Chart-Rendering dynamisiert werden. Dies beinhaltet das Zeichnen mehrerer Linien für Overlays (SMA, EMA, BBANDS) und die Nutzung separater Preisskalen für Oszillatoren (RSI), die andere Wertebereiche haben.

### Q: Warum wurden keine Signale ausgegeben, obwohl Indikatoren konfiguriert waren?
*   **A:** Die Backend-Signal-Engine kannte die neu hinzugefügten Indikatoren (EMA, BBANDS) noch nicht und ignorierte sie bei der Konsensbildung. Zudem ist die Konsens-Logik sehr streng: Alle aktiven Indikatoren (und optional die KI) müssen ein Signal in dieselbe Richtung geben, damit ein Trade ausgelöst wird. Die Engine wurde nun erweitert, um alle Indikatoren zu berücksichtigen und detailliertere Begründungen für fehlende Signale zu liefern.

### Q: Was ist der Unterschied zwischen Indikatorberechnung im Frontend (React) und Backend (Java)?
*   **A:**
    *   **Frontend (React):** Berechnet Indikatoren in JavaScript für das **visuelle Replay** und die **Echtzeit-Anzeige im Chart**. Dies ermöglicht sofortiges Feedback und Interaktivität, da keine Serveranfrage nötig ist.
    *   **Backend (Java):** Berechnet Indikatoren in Java für den **schnellen Backtest** und den **Live-Handel**. Dies gewährleistet Performance, Stabilität und die Ausführung der eigentlichen Handelslogik, unabhängig vom Browser.
    *   **Synchronisation:** Durch die Übertragung der Indikatorkonfiguration vom Frontend an das Backend stellen wir sicher, dass beide Systeme mit denselben Parametern arbeiten und die Strategie konsistent ist.

---
*Zuletzt aktualisiert: 30. Juli 2024*
