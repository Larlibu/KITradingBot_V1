# ML-Service (FastAPI)

Dieser Service liefert die ML-Funktionen fuer den KITradingBot:

- Einzelvorhersage
- Batch-Vorhersage (Backtesting)
- asynchrones Modelltraining
- asynchrones Marktdaten-Update
- Candles aus lokalem CSV-Bestand

## Voraussetzungen

- Python 3.10+
- Abhaengigkeiten aus `requirements.txt`

## Installation

Im Ordner `ml-service`:

```bash
pip install -r requirements.txt
```

## Start

```bash
python -m uvicorn api:app --host 127.0.0.1 --port 8000
```

Health:

- `GET http://localhost:8000/health`

## Daten und Modell

- Rohdaten: `ml-service/data/raw_1h.csv` (oder `raw_1m.csv`)
- Trainingsdaten: `ml-service/data/training_data.csv`
- Modell: `ml-service/model.pkl`

## Labeling-Strategie (aktuell)

Die Datenaufbereitung in `data_processor.py` nutzt:

- `horizon=3` (Vorhersage ueber 3 Kerzen)
- `min_move_pct=0.20`

Kleine Seitwaertsbewegungen innerhalb dieser Schwelle werden verworfen.

## ML-Feature-Schema (Reihenfolge)

Die Inferenz erwartet aktuell 8 Features in dieser Reihenfolge:

1. `close`
2. `sma`
3. `rsi`
4. `close_minus_sma`
5. `return_1`
6. `return_3`
7. `volatility_6`
8. `close_minus_ema`

Zusatz:
- Das finale RandomForest-Modell wird per `CalibratedClassifierCV` (sigmoid) kalibriert,
  damit `confidence`-Werte belastbarer sind.

## Endpunkte

### Health
- `GET /health`

Beispiel:

```json
{ "status": "ok" }
```

### Candles
- `GET /data/candles?limit=500`

Beispiel:

```json
[
  {
    "timestamp": "2026-05-18 10:00:00",
    "open": 64000.0,
    "high": 64200.0,
    "low": 63950.0,
    "close": 64120.0,
    "volume": 123.45
  }
]
```

### Einzelvorhersage
- `POST /predict`

Request:

```json
{
  "features": [64120.0, 63980.0, 58.2, 140.0, 0.0012, 0.0034, 0.0021, 120.5]
}
```

Response:

```json
{
  "label": "STEIGT",
  "confidence": 0.73
}
```

### Batch-Vorhersage
- `POST /predict/batch`

Request:

```json
{
  "batch": [
    [64120.0, 63980.0, 58.2, 140.0, 0.0012, 0.0034, 0.0021, 120.5],
    [64020.0, 64050.0, 47.1, -30.0, -0.0016, -0.0030, 0.0028, -15.2]
  ]
}
```

Response:

```json
{
  "predictions": [
    { "label": "STEIGT", "confidence": 0.73 },
    { "label": "FAELLT", "confidence": 0.61 }
  ]
}
```

### Training (asynchron)
- `POST /train`
- `GET /train/status`

Beispiel:

```json
{
  "status": "running",
  "last_run": "2026-05-18 20:00:00",
  "error_message": null
}
```

### Datenupdate (asynchron)
- `POST /data/update`
- `GET /data/status`

Beispiel:

```json
{
  "status": "success",
  "last_update": "2026-05-18 20:05:00",
  "error_message": null
}
```

## Training manuell starten

Wenn `training_data.csv` vorhanden ist:

```bash
python train.py
```

## Typische Fehler

- `500` bei `/predict` oder `/predict/batch`:
  - `model.pkl` fehlt oder ist inkompatibel.
- `404` bei `/data/candles`:
  - es sind noch keine Rohdaten vorhanden.
- `status=error` bei Training oder Datenupdate:
  - Netzwerk-/Binance-Problem oder fehlende Python-Abhaengigkeit.

## Integration mit Backend

Der Java-Backend-Service erwartet den ML-Service standardmaessig unter:

- `http://localhost:8000`

Konfigurierbar ueber `trading.ml-service-url` im Backend.
