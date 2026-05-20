# 📝 Projekt-Log: KITradingBot (Sitzung 2026-05-20)

## 🛠 1. Durchgeführte Arbeiten & Refactorings

### 1.1 Synchronisierung der Analyse-Parameter
*   **SMA-Periode:** Es wurde eine Inkonsistenz festgestellt (ML-Modell trainiert auf 20, Frontend/Backend nutzten 14). Alle SMA-Standardwerte wurden systemweit auf **20** synchronisiert, um die Vorhersagegenauigkeit der KI im Live-Betrieb und Backtest sicherzustellen.
*   **Entkopplung der ML-Features:** Die Java-Klasse `IndicatorFeaturePipeline` wurde dahingehend korrigiert, dass ML-Features (für die KI) nun immer mit den fixen Trainings-Perioden berechnet werden, unabhängig davon, was der Benutzer im Frontend-Dashboard konfiguriert.

### 1.2 Korrektur der Indikator-Mathematik
*   **EMA-Abgleich:** Die Java-Implementierung des `EmaIndicator` wurde von einer "Window-basierten" auf eine "globale" Berechnung umgestellt. Damit ist sie nun mathematisch identisch mit der Logik in JavaScript (Frontend) und Python (ML-Service).

### 1.3 Neue Funktionen (Frontend-Backtesting)
*   **VWAP-Implementierung:** Der **Volume Weighted Average Price (VWAP)** wurde als neuer Indikator in die `tradingViewIndicators.js` integriert.
    *   Berechnungslogik für kumulatives Volumen und Preis-Volumen-Produkt hinzugefügt.
    *   Integration in das UI-Panel und die Signal-Engine (Preis > VWAP = BUY, Preis < VWAP = SELL).

---

## 🧠 2. Wissensdatenbank & Erklärungen

### Q: Ist die Regel "Kurs > EMA = Aufwärtstrend" korrekt?
*   **A:** Ja, das ist ein Standardprinzip der technischen Analyse. Die EMA fungiert hierbei als dynamische Unterstützung (im Aufwärtstrend) oder Widerstand (im Abwärtstrend). Für ein valideres Signal sollte zudem die Steigung der EMA-Linie selbst berücksichtigt werden.

### Q: Warum können Indikatoren nicht direkt von TradingView geladen werden?
*   **A:** TradingView nutzt die proprietäre Sprache **Pine Script**, während der Bot JavaScript und Java nutzt. Zudem schützt TradingView seine Skripte vor externem Zugriff. Die Lösung im Projekt ist die manuelle Portierung der mathematischen Logik, um Unabhängigkeit und Backtesting-Fähigkeit zu behalten.

### Q: Wie reagiert die KI auf Strategieänderungen im Frontend?
*   **A:** Die KI fungiert als **"Sicherheits-Wähler"**. Sie wurde auf spezifische Muster (Features) trainiert und behält diese Sichtweise bei. Wenn eine Frontend-Strategie ein Signal gibt, prüft die KI, ob dieses Signal mit ihren gelernten Mustern übereinstimmt. Für maximale Performance sollten die Backtesting-Indikatoren und die ML-Features langfristig synchronisiert werden.

---

## 📋 3. Status & Nächste Schritte
*   **Status:** Alle drei Dienste (Frontend, Backend, ML-Service) sind nun mathematisch synchronisiert. Die Strategie-Grundlage im Frontend ist bereit für erweiterte Tests.
*   **Nächster Schritt:** Testen der VWAP-Strategie im Backtest und ggf. Anpassung der Konsens-Logik (z.B. Einführung eines gewichteten Votings oder eines Mehrheitsentscheids).

---
*Zuletzt aktualisiert: 20. Mai 2026*
