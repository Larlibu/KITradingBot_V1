# Backend (Spring Boot)

Dieses Modul ist das zentrale API- und Orchestrierungs-Backend des KITradingBot.

## Aufgaben

- Marktdaten abrufen (`/api/market`)
- Signale berechnen (`/api/signal`)
- Backtests ausfuehren (`/api/backtest`)
- Simulierte Trades verwalten (`/api/trades/*`)
- ML-Service fuer Vorhersage, Training und Datenupdate anbinden (`/api/predict/*`)

## Voraussetzungen

- Java 21
- Maven 3.9+

## Start

Im Ordner `backend`:

```bash
mvn spring-boot:run
```

Standard-Port:

- `http://localhost:8080`

## Build und Test

```bash
mvn test
mvn package
```

JavaDoc:

```bash
mvn javadoc:javadoc
```

Ausgabe:

- `target/site/apidocs/index.html`

## Wichtige Konfiguration

Datei:

- `src/main/resources/application.properties`

Wichtige Keys:

- `server.port=8080`
- `trading.symbol=BTCUSDT`
- `trading.default-market-limit=200`
- `trading.ml-service-url=http://localhost:8000`
- `trading.binance-url=https://api.binance.com/api/v3/klines`

## API-Ueberblick

### Markt und Signale
- `GET /api/market`
- `POST /api/signal`
- `POST /api/backtest`

### Trades (Simulation)
- `GET /api/trades`
- `POST /api/trades/start`
- `POST /api/trades/stop`
- `POST /api/trades/reset`

### ML-Integration
- `POST /api/predict`
- `POST /api/predict/train`
- `GET /api/predict/train/status`
- `POST /api/predict/data/update`
- `GET /api/predict/data/status`

## Interne Struktur

- `controller/` REST-Endpunkte
- `service/` Geschaeftslogik
- `signal/` Signal-Engine (HybridSignalEngine)
- `trading/` Simulierte Trading-Engine
- `backtesting/` Backtest-Engine
- `integration/` HTTP-Client zum ML-Service
- `repository/` Datenzugriff (inkl. In-Memory Trades)
- `config/` CORS, Properties, Exception-Handling

## Hinweise

- Trades sind aktuell in-memory und nicht persistent.
- Bei ML-Ausfall nutzt die Prediction-Logik einen Backend-Fallback.
