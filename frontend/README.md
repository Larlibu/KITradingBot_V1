# Frontend (React + Vite)

Dieses Modul stellt das Dashboard fuer den KITradingBot bereit.

## Aufgaben

- Live- und Analyse-Ansicht
- Visualisierung von Preis, Signal, KI-Konfidenz und Indikatoren
- Steuerung fuer simuliertes Trading
- Replay-Analyse und Backtest-UI
- Trigger fuer ML-Training und Datenupdate

## Voraussetzungen

- Node.js 18+
- npm

## Installation

Im Ordner `frontend`:

```bash
npm install
```

## Start (Dev)

```bash
npm run dev
```

Standard-URL:

- `http://localhost:5173`

Das Frontend erwartet das Backend standardmaessig unter:

- `http://localhost:8080/api`

Konfiguration in:

- `src/api/client.js`

## Build

```bash
npm run build
npm run preview
```

Build-Ausgabe:

- `dist/`

## React-Doku (HTML)

```bash
cmd /c npx documentation build src/**/*.js src/**/*.jsx -f html -o reactdoc
```

Ausgabe:

- `reactdoc/index.html`

## Modus-Ueberblick

### Live-Modus
- Bot-Steuerung (Start/Stop)
- Optional: KI-Konsens erzwingen
- Laufende simulierte Trades

### Analyse-Modus
- Indikator-Konfiguration
- Visuelles Replay (Play/Pause/Schritt/Geschwindigkeit)
- Schnell-Analyse (Backtest)
- ML-Modell-Training und Marktdaten-Update

## Struktur

- `src/App.jsx` Einstiegspunkt
- `src/hooks/useAppLogic.js` zentraler UI-Controller
- `src/components/` View-Komponenten
- `src/api/` API-Client und Endpunktfunktionen
- `src/indicators/` clientseitige Indikator-/Replay-Logik

## Hinweise

- Das Frontend nutzt Polling fuer Status- und Live-Daten.
- Die tatsaechliche Signallogik fuer Live/Backtest liegt im Backend; das Frontend steuert und visualisiert.
