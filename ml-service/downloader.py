"""
Modul zum Herunterladen historischer Marktdaten von der Binance API.

Stellt Funktionen bereit, um Candlestick-Daten für ein bestimmtes Symbol
und Intervall über einen definierten Zeitraum abzurufen und lokal zu speichern.
"""

import requests
import pandas as pd
import time
from datetime import datetime, timedelta
import os
from pathlib import Path
from typing import List, Dict, Any

# --- Konfiguration ---
# Das Handelssymbol, für das Daten heruntergeladen werden sollen.
SYMBOL: str = "BTCUSDT"
# Das Verzeichnis, in dem die heruntergeladenen Daten gespeichert werden.
DATA_DIR: Path = Path(__file__).resolve().parent / "data"
# Die Basis-URL der Binance API für Klines (Candlestick-Daten).
BINANCE_URL: str = "https://api.binance.com/api/v3/klines"
# Das maximale Limit an Kerzen pro API-Anfrage (Binance-Limit).
LIMIT: int = 1000

# Spaltennamen für die von der Binance API zurückgegebenen Daten.
# Diese Reihenfolge ist fest von der Binance API vorgegeben.
COLUMNS: List[str] = [
    "open_time", "open", "high", "low", "close", "volume",
    "close_time", "quote_volume", "trades",
    "taker_buy_base", "taker_buy_quote", "ignore"
]

def fetch_binance_data(symbol: str, interval: str, lookback_days: int) -> Path:
    """
    Lädt historische Candlestick-Daten von der Binance API für ein gegebenes Symbol und Intervall.
    Die Daten werden in einer CSV-Datei im 'data'-Verzeichnis gespeichert.

    Args:
        symbol (str): Das Handelspaar (z.B. "BTCUSDT").
        interval (str): Das Zeitintervall der Kerzen (z.B. "1h" für 1 Stunde, "1d" für 1 Tag).
        lookback_days (int): Die Anzahl der Tage in die Vergangenheit, für die Daten geladen werden sollen.

    Returns:
        Path: Der Pfad zur lokal gespeicherten CSV-Datei mit den Rohdaten.
    """
    # Sicherstellen, dass das Datenverzeichnis existiert.
    os.makedirs(DATA_DIR, exist_ok=True)

    all_data: List[List[Any]] = []

    # Berechnet den Startzeitpunkt (in Millisekunden seit der Epoche) basierend auf lookback_days.
    since: datetime = datetime.utcnow() - timedelta(days=lookback_days)
    start_ms: int = int(since.timestamp() * 1000)
    # Aktueller Endzeitpunkt (in Millisekunden seit der Epoche).
    end_ms: int = int(datetime.utcnow().timestamp() * 1000)

    print(f"Lade {interval}-Daten für {symbol} von Binance API...")

    # Schleife, um Daten in Chunks von LIMIT Kerzen abzurufen, bis der Endzeitpunkt erreicht ist.
    while start_ms < end_ms:
        # Parameter für die API-Anfrage.
        params: Dict[str, Any] = {"symbol": symbol, "interval": interval, "startTime": start_ms, "limit": LIMIT}

        # Führt die GET-Anfrage an die Binance API aus.
        resp: requests.Response = requests.get(BINANCE_URL, params=params, timeout=10)
        resp.raise_for_status()  # Löst eine HTTPError für schlechte Antworten (4xx oder 5xx) aus.

        chunk: List[List[Any]] = resp.json()
        if not chunk:
            break  # Keine weiteren Daten verfügbar, Schleife beenden.

        all_data.extend(chunk)
        # Aktualisiert den Startzeitpunkt für die nächste Anfrage, um Duplikate zu vermeiden.
        start_ms = chunk[-1][0] + 1

        # Kurze Pause, um das Rate-Limit der Binance API nicht zu überschreiten.
        time.sleep(0.1)

    # Erstellt einen Pandas DataFrame aus den gesammelten Daten.
    df: pd.DataFrame = pd.DataFrame(all_data, columns=COLUMNS)

    # Konvertiert den 'open_time'-Timestamp in ein lesbares Datumsformat.
    df["open_time"] = pd.to_datetime(df["open_time"], unit="ms")

    # Konvertiert relevante Spalten in den float-Datentyp.
    for col in ["open", "high", "low", "close", "volume"]:
        df[col] = df[col].astype(float)

    # Speichert den DataFrame als CSV-Datei.
    output_file: Path = DATA_DIR / f"raw_{interval}.csv"
    df.to_csv(output_file, index=False)
    print(f"Daten heruntergeladen und gespeichert: {output_file} ({len(df)} Zeilen)")
    return output_file
