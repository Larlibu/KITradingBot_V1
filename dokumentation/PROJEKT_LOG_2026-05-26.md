# 📝 Projekt-Log: KITradingBot (Sitzung 2026-05-26)

## 🛠 1. Durchgeführte Arbeiten & Refactorings

### 1.1 Migration auf ta4j-Framework
*   **Zentralisierung der Berechnungen:** Die gesamte technische Indikator-Logik wurde von manuellen Berechnungen auf die Industrie-Standard-Bibliothek **ta4j** umgestellt.
*   **Konsistenz:** Durch die Nutzung von ta4j im Backend ist sichergestellt, dass Live-Signale und Backtesting-Ergebnisse auf derselben mathematischen Grundlage basieren.
*   **Strategie-Migration:** Handelsstrategien (z.B. SMA-Crossover, RSI-Oversold) werden nun über ta4j-`Rules` und `Strategy`-Objekte abgebildet, was die Erweiterbarkeit massiv verbessert.

### 1.2 Dynamische Feature-Pipeline & UI-Integration
*   **Parameter-Synchronisierung:** Die `IndicatorFeaturePipeline` wurde dahingehend optimiert, dass sie nun dynamisch die in der UI konfigurierten Parameter (z.B. SMA-Länge, RSI-Periode) für die Erstellung der ML-Features nutzt.
*   **Fallback-Logik:** Falls keine Parameter von der UI übergeben werden, nutzt das System bewährte Standardwerte (SMA 20, RSI 14, EMA 21).

### 1.3 Optimierung des Backtesting-Moduls
*   **Backtest-Engine:** Die `SimpleBacktestEngine` wurde komplett refaktoriert. Sie nutzt nun die ta4j-Trading-Engine zur Simulation von Trades.
*   **Indikator-Historie:** Das Backtest-Ergebnis enthält nun eine vollständige Historie aller berechneten Indikatorwerte, die im Frontend zur Visualisierung (z.B. in Tooltips) genutzt werden können.
*   **ML-Integration:** KI-Vorhersagen werden nun nahtlos als `MlRule` in die ta4j-Strategie eingebunden, was einen echten Hybrid-Backtest (Indikatoren + ML) ermöglicht.

### 1.4 Frontend- & API-Anpassungen
*   **API-Protokoll:** Die Endpunkte `/api/signal` und `/api/backtest` wurden von GET auf **POST** umgestellt, um komplexe Indikator-Konfigurationen im Request-Body übertragen zu können.
*   **UI-Stabilität:** Das Frontend nutzt nun die vom Backend berechneten Indikatorwerte und Entscheidungen direkt (`evaluateDecisionAtIndex`), anstatt diese lokal neu zu berechnen. Dies behebt Diskrepanzen zwischen Chart-Darstellung und Signal-Logik.

---

## 🧠 2. Wissensdatenbank & Erklärungen

### Q: Warum wurde ta4j eingeführt?
*   **A:** Manuelle Implementierungen von Indikatoren (wie EMA oder Bollinger Bands) sind fehleranfällig, insbesondere bei der Handhabung von Precision und Warmup-Perioden. ta4j bietet eine geprüfte, robuste Basis und erlaubt komplexe Regel-Kombinationen (z.B. "EMA Steigung > 0 UND RSI < 30").

### Q: Was bedeutet "Dynamische Feature-Pipeline" für die KI?
*   **A:** Früher war die KI auf feste Eingaben trainiert. Jetzt kann sie (innerhalb gewisser Grenzen) Features verarbeiten, die auf den aktuell vom Benutzer gewählten Einstellungen basieren. Dies macht das System flexibler für verschiedene Marktphasen.

### Q: Wie werden die "Votes" im Backtest jetzt berechnet?
*   **A:** Alle "Stimmen" kommen nun direkt aus dem Backend. Für jeden Schritt im Backtest liefert das Backend nicht nur die finale Entscheidung (BUY/SELL), sondern auch die zugrundeliegenden Werte aller aktiven Indikatoren.

---

## 📋 3. Status & Nächste Schritte
*   **Status:** Die Kern-Architektur ist durch die Zentralisierung im Backend deutlich stabiler und wartbarer. Die Diskrepanzen zwischen UI und Logik wurden eliminiert.
*   **Nächster Schritt:** Implementierung erweiterter ta4j-Strategien (z.B. Trailing Stop Loss) und Verbesserung der Performance bei sehr großen Backtests durch Caching von BarSeries.

---
*Zuletzt aktualisiert: 26. Mai 2026*
