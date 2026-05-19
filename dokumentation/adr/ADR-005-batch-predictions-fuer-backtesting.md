# ADR-005: Batch-Predictions fuer Backtesting

- Status: Accepted
- Datum: 2026-05-18
- Entscheider: Projektteam KITradingBot

## Kontext
Backtests koennen hunderte bis tausende Zeitpunkte enthalten. Einzelne ML-HTTP-Calls pro Tick verursachen hohen Overhead und langsame Runs.

## Entscheidung
Das Backend sendet fuer Backtests Feature-Vektoren gesammelt an den ML-Service (`/predict/batch`) und verarbeitet die Antwort als Liste.

## Konsequenzen
### Positiv
- Deutlich weniger HTTP-Overhead.
- Schnellere Backtests bei grosser Kerzenzahl.
- Bessere Nutzererfahrung im Analysemodus.

### Negativ
- Hoeherer Speicherbedarf fuer vorbereitete Feature-Batches.
- Fehler im Batch wirken auf gesamten Backtest-Lauf.

## Alternativen
- N Einzel-Requests ueber `/predict`.
- Komplette Inferenz lokal im Backend ohne separaten ML-Service.
