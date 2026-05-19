# Benutzerhandbuch: KITradingBot

Willkommen beim KITradingBot.  
Die Anwendung kombiniert technische Indikatoren mit optionaler KI-Vorhersage und bietet Live-Simulation sowie Analyse/Backtesting.

---

## 1. Voraussetzungen

- Java 21
- Node.js + npm
- Python 3.10+

---

## 2. Start und Stop

### Alles automatisch starten
Im Projekt-Root:

- `start-all.cmd` (per Doppelklick)
- oder `powershell -ExecutionPolicy Bypass -File .\start-all.ps1`

Gestartet werden:
- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`
- ML-Service: `http://localhost:8000`

### Alles stoppen
Im Projekt-Root:

- `stop-all.cmd`
- oder `powershell -ExecutionPolicy Bypass -File .\stop-all.ps1`

Logs liegen unter `.run/`.

---

## 3. Aufbau der Anwendung

Oben im Header kannst du zwischen zwei Modi wechseln:

- `Live-Modus`
- `Analyse-Modus`

### Statusleiste
Im Dashboard wird der Dienststatus angezeigt:

- `Backend` (online/offline)
- `ML-Dienst` (online/offline)

### Kennzahlenkarten
Die Karten zeigen u. a.:

- aktuellen Preis
- aktuelles Signal (`KAUFEN`, `VERKAUFEN`, `WARTEN`)
- KI-Konfidenz
- Werte der aktiven Indikatoren (dynamisch je Konfiguration)

---

## 4. Live-Modus

Im Live-Modus steht links das Panel `Bot-Steuerung (Live)`:

- `Trading starten`
- `Stop`
- `KI-Konsens erzwingen` (nur handeln, wenn KI und Technik uebereinstimmen)

Unten werden ausgefuehrte simulierte Trades in der Tabelle angezeigt.

---

## 5. Analyse-Modus

Im Analyse-Modus gibt es drei zentrale Bereiche:

1. `KI-Modell Optimierung`
   - `Marktdaten aktualisieren`
   - `KI jetzt trainieren`
   - jeweiliger Status (`running/success/error`) wird angezeigt

2. `Konfiguration & Strategie`
   - KI-Konsens aktivieren/deaktivieren
   - Indikatoren hinzufuegen/entfernen
   - Parameter direkt anpassen

3. `Analyse & Replay`
   - `Schnell-Analyse (Backtest)` fuehrt einen Backend-Backtest aus
   - `Visuelles Replay` startet den Replay-Modus
   - Replay-Steuerung mit:
     - Schritt zurueck / vor
     - `Abspielen` / `Pause`
     - Geschwindigkeitsregler

Waehrend des Replay kann bei BUY/SELL ein Signal-Dialog erscheinen; nach Bestaetigung laeuft das Replay weiter.

---

## 6. Trades-Tabelle

Die Tabelle `Ausgefuehrte Trades (Simulation)` zeigt:

- ID
- Zeitpunkt
- Seite (KAUF/VERKAUF)
- Preis
- Menge
- GuV
- Kapital nach Trade

Im Live-Modus: Live-Simulations-Trades.  
Im Analyse-Modus: Trades aus dem letzten Backtest.

---

## 7. Fehlerbehebung

### Dashboard zeigt Dienste als offline
- Warte nach dem Start kurz, bis alle Services hochgefahren sind.
- Pruefe `.run/backend.err.log`, `.run/ml-service.err.log`, `.run/frontend.err.log`.

### Keine Signale oder nur WARTEN
- In der Aufwaermphase fehlen oft noch genug Kerzen fuer alle Indikatoren.
- Pruefe, ob KI-Konsens aktiv ist; dieser macht Signale strenger.

### Training oder Datenupdate bleibt auf Fehler
- Pruefe, ob der ML-Service auf `http://localhost:8000/health` erreichbar ist.
- Pruefe Python-Abhaengigkeiten aus `ml-service/requirements.txt`.
