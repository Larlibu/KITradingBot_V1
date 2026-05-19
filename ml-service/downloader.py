"""
Modul zum Herunterladen historischer Marktdaten von der Binance API.

Stellt Funktionen bereit, um Candlestick-Daten (OHLCV) für ein bestimmtes Symbol
und Intervall über einen definierten Zeitraum abzurufen und lokal als CSV zu speichern.
"""

import requests
import pandas as pd
import time
from datetime import datetime, timedelta
import os
from pathlib import Path
from typing import List, Dict, Any

# --- Statische Konfiguration ---
SYMBOL: str = "BTCUSDT"
DATA_DIR: Path = Path(__file__).resolve().parent / "data"
BINANCE_URL: str = "https://api.binance.com/api/v3/klines"
LIMIT: int = 1000 # Binance erlaubt max 1000 Kerzen pro Request

# Spaltendefinition der Binance API Antwort
COLUMNS: List[str] = [
    "open_time", "open", "high", "low", "close", "volume",
    "close_time", "quote_volume", "trades",
    "taker_buy_base", "taker_buy_quote", "ignore"
]

def fetch_binance_data(symbol: str, interval: str, lookback_days: int) -> Path:
    """
    Lädt historische Candlestick-Daten von Binance herunter.
    Unterstützt Paging, falls mehr als 1000 Kerzen angefordert werden.

    Args:
        symbol: Handelspaar (z.B. "BTCUSDT").
        interval: Zeitrahmen (z.B. "1h", "1m").
        lookback_days: Wie viele Tage in die Vergangenheit geladen werden sollen.

    Returns:
        Path: Pfad zur gespeicherten CSV-Datei.
    """
    # Sicherstellen, dass der Speicherort existiert
    os.makedirs(DATA_DIR, exist_ok=True)

    all_data: List[List[Any]] = []

    # Zeitfenster berechnen (Binance erwartet Millisekunden)
    since: datetime = datetime.utcnow() - timedelta(days=lookback_days)
    start_ms: int = int(since.timestamp() * 1000)
    end_ms: int = int(datetime.utcnow().timestamp() * 1000)

    print(f"Starte Download: {symbol} [{interval}] für die letzten {lookback_days} Tage...")

    # Paging-Schleife
    while start_ms < end_ms:
        params: Dict[str, Any] = {
            "symbol": symbol,
            "interval": interval,
            "startTime": start_ms,
            "limit": LIMIT
        }

        try:
            # API Request absetzen
            resp: requests.Response = requests.get(BINANCE_URL, params=params, timeout=15)
            resp.raise_for_status()
            
            chunk: List[List[Any]] = resp.json()
            if not chunk:
                break # Keine weiteren Daten verfügbar
            
            all_data.extend(chunk)
            
            # Startzeit für den nächsten Chunk ist das Ende des aktuellen + 1ms
            start_ms = chunk[-1][0] + 1
            
            # Rate-Limiting beachten (Binance ist streng)
            time.sleep(0.1)
            
            if len(all_data) % 5000 == 0:
                print(f"... {len(all_data)} Kerzen geladen ...")

        except Exception as e:
            print(f"Fehler beim Abruf der Daten: {e}")
            break

    # --- Datenverarbeitung ---
    df: pd.DataFrame = pd.DataFrame(all_data, columns=COLUMNS)

    # Timestamp in lesbares Datum konvertieren
    df["open_time"] = pd.to_datetime(df["open_time"], unit="ms")

    # Numerische Werte als Float sicherstellen
    numeric_cols = ["open", "high", "low", "close", "volume"]
    for col in numeric_cols:
        df[col] = df[col].astype(float)

    # Speichern der Rohdaten
    output_file: Path = DATA_DIR / f"raw_{interval}.csv"
    df.to_csv(output_file, index=False)
    
    print(f"Download abgeschlossen. Datei gespeichert: {output_file} ({len(df)} Zeilen)")
    return output_file
