# KITradingBot

KITradingBot ist eine lokale Trading- und Analyseplattform fuer Krypto-Marktdaten.  
Die Anwendung kombiniert technische Indikatoren mit optionaler ML-Vorhersage, inklusive Replay, Backtesting und simuliertem Trading.

## Architektur (Monorepo)

```text
KITradingBot/
  backend/       Spring Boot REST-API (Signale, Backtest, Trading)
  ml-service/    FastAPI ML-Service (Predict, Training, Datenupdate)
  frontend/      React/Vite Dashboard
  dokumentation/ Arc42, ADRs, Projektlogs
```

## Voraussetzungen

- Java 21
- Node.js + npm
- Python 3.10+

## Schnellstart

Im Projekt-Root:

- `start-all.cmd`
- oder `powershell -ExecutionPolicy Bypass -File .\start-all.ps1`

Danach:
- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`
- ML-Service: `http://localhost:8000`

Logs: `.run/`

## Stoppen

Im Projekt-Root:

- `stop-all.cmd`
- oder `powershell -ExecutionPolicy Bypass -File .\stop-all.ps1`

## Manueller Start (3 Terminals)

1. ML-Service
   - `cd ml-service`
   - `python train.py` (optional)
   - `python -m uvicorn api:app --host 127.0.0.1 --port 8000`
2. Backend
   - `cd backend`
   - `mvn spring-boot:run`
3. Frontend
   - `cd frontend`
   - `npm.cmd install`
   - `npm.cmd run dev -- --host 127.0.0.1 --port 5173`

## Kernfunktionen

- Live-Signal mit Indikator-Konsens (`BUY`/`SELL`/`HOLD`)
- Optionaler KI-Konsens
- Simulierte Trade-Ausfuehrung (Paper Trading)
- Backtesting mit Batch-ML-Predictions
- Visuelles Replay im Analysemodus
- ML-Datenupdate und Modelltraining ueber UI

## Wichtige API-Endpunkte

- `GET /api/market`
- `POST /api/signal`
- `POST /api/backtest`
- `GET /api/trades`
- `POST /api/trades/start`
- `POST /api/trades/stop`
- `POST /api/trades/reset`
- `POST /api/predict`
- `POST /api/predict/train`
- `GET /api/predict/train/status`
- `POST /api/predict/data/update`
- `GET /api/predict/data/status`

## Dokumentation

- Benutzerhandbuch: [BENUTZERHANDBUCH.md](./BENUTZERHANDBUCH.md)
- Architektur (arc42): [dokumentation/ARC42.md](./dokumentation/ARC42.md)
- ADRs: [dokumentation/adr/README.md](./dokumentation/adr/README.md)
