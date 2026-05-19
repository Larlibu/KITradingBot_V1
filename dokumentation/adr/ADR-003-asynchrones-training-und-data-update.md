# ADR-003: Asynchrones Training und Datenupdate im ML-Service

- Status: Accepted
- Datum: 2026-05-18
- Entscheider: Projektteam KITradingBot

## Kontext
Training und Datenupdate sind potenziell langlaufend. Synchrone API-Aufrufe wuerden Timeouts und blockierte User-Flows verursachen.

## Entscheidung
Der ML-Service fuehrt Training und Datenupdate asynchron per BackgroundTask aus und stellt Status-Endpunkte bereit:
- `POST /train`, `GET /train/status`
- `POST /data/update`, `GET /data/status`

Das Backend reicht diese Aufrufe durch.

## Konsequenzen
### Positiv
- UI bleibt reaktionsfaehig.
- Kein langer HTTP-Request fuer rechenintensive Prozesse.
- Fortschritt/Status ist abfragbar.

### Negativ
- Komplexere Zustandslogik (running/success/error).
- Fehler treten zeitversetzt auf und brauchen gutes Monitoring/Logging.

## Alternativen
- Voll synchrones Training im Request.
- Externer Job-Runner/Queue (fuer aktuellen lokalen Scope zu aufwendig).
