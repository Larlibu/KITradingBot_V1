# ADR-002: Backend orchestriert Signallogik

- Status: Accepted
- Datum: 2026-05-18
- Entscheider: Projektteam KITradingBot

## Kontext
Signale muessen konsistent fuer Live-Modus, Trade-Ausfuehrung und Backtesting berechnet werden. Die Frontend-Darstellung darf nicht die Systemwahrheit definieren.

## Entscheidung
Die zentrale Signallogik liegt im Backend:
- Marktdaten abrufen
- Features berechnen
- optional ML-Vorhersage einholen
- finales Signal erzeugen
- im Live-Betrieb simulierte Trades ausfuehren

Das Frontend steuert und visualisiert nur.

## Konsequenzen
### Positiv
- Einheitliche Entscheidungslogik fuer alle Clients.
- Bessere Testbarkeit der Business-Regeln im Backend.
- Frontend bleibt austauschbarer.

### Negativ
- Hoehere Backend-Verantwortung und Komplexitaet.
- Frontend-Logik fuer Replay kann von Backend-Logik abweichen, wenn nicht synchron gehalten.

## Alternativen
- Signallogik im Frontend mit nur passivem Backend.
- Doppelte Signallogik in Frontend und Backend (verworfen wegen Drift-Risiko).
