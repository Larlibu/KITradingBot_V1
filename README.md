# KITradingBot

KITradingBot ist eine KI-gestützte Krypto-Trading-Plattform mit:

- Java Spring Boot Backend (MVC + modulare Architektur)
- Python ML-Service für Training und Inferenz
- React Dashboard Frontend
- Backtesting und simuliertem Trade-Execution-Flow

## Projektstruktur (Monorepo)

```text
KITradingBot/
  backend/      # Spring Boot REST-API, Signal- und Trading-Engine
  ml-service/   # FastAPI Vorhersageservice + Trainingsskripte
  frontend/     # React Dashboard (Vite)
```

## Schnellstart (ein Befehl)

Im Projekt-Root ausführen:

- `powershell -ExecutionPolicy Bypass -File .\start-all.ps1`
- oder per Doppelklick: `start-all.cmd`

Optional ohne Training:

- `powershell -ExecutionPolicy Bypass -File .\start-all.ps1 -NoTrain`

Optional mit Pause am Ende (Fenster bleibt offen):

- `powershell -ExecutionPolicy Bypass -File .\start-all.ps1 -PauseOnExit`

Das Script startet automatisch:

- ML-Service auf `http://localhost:8000`
- Backend auf `http://localhost:8080`
- Frontend auf `http://localhost:5173`

Logs liegen unter `.run/`.

## Anwendung beenden

Im Projekt-Root ausführen:

- `powershell -ExecutionPolicy Bypass -File .\stop-all.ps1`
- oder per Doppelklick: `stop-all.cmd`

Optional mit Pause am Ende:

- `powershell -ExecutionPolicy Bypass -File .\stop-all.ps1 -PauseOnExit`

## Manueller Start (3 Terminals)

1. ML-Service starten:
   - `cd ml-service`
   - `python train.py`
   - `python -m uvicorn api:app --host 127.0.0.1 --port 8000`
2. Backend starten:
   - `cd backend`
   - `mvn spring-boot:run`
3. Frontend starten:
   - `cd frontend`
   - `npm.cmd install`
   - `npm.cmd run dev -- --host 127.0.0.1 --port 5173`

## Wichtige REST-Endpunkte

- `GET /api/market`
- `GET /api/signal`
- `POST /api/predict`
- `GET /api/trades`
- `GET /api/backtest`
