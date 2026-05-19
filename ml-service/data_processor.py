"""
Modul zur Vorbereitung von Marktdaten fuer das Machine-Learning-Modell.

Enthaelt Funktionen zur Berechnung technischer Indikatoren (SMA, RSI, EMA, Volatilität)
und zur Generierung robuster Ziel-Labels aus Rohdaten (Candlesticks) unter
Berücksichtigung von Rauschunterdrückung.
"""

from pathlib import Path
import numpy as np
import pandas as pd

# --- Konfiguration der Indikatoren ---
# WICHTIG: Diese Perioden muessen mit der aktuellen Java-FeaturePipeline
# konsistent bleiben, damit Training und Inferenz dieselben Features sehen.
SMA_PERIOD = 20
RSI_PERIOD = 14
EMA_PERIOD = 21
VOLATILITY_PERIOD = 6

# --- Labeling-Konfiguration zur Rauschreduktion ---
# horizon: Anzahl der Kerzen in die Zukunft, auf die wir schauen
DEFAULT_LABEL_HORIZON = 3
# min_move_pct: Mindestbewegung in Prozent, damit ein Label vergeben wird (gegen Seitwärtsrauschen)
DEFAULT_MIN_MOVE_PCT = 0.20


def calculate_rsi(series: pd.Series, period: int = RSI_PERIOD) -> pd.Series:
    """
    Berechnet den Relative Strength Index (RSI).
    Der RSI ist ein Momentum-Oszillator, der die Geschwindigkeit und Veränderung von Preisbewegungen misst.

    Args:
        series: Schlusskurse.
        period: Berechnungszeitraum (Standard: 14).

    Returns:
        pd.Series: RSI-Werte zwischen 0 und 100.
    """
    # Preisänderungen berechnen
    delta = series.diff()
    
    # Gewinne (gain) und Verluste (loss) trennen
    gain = (delta.where(delta > 0, 0)).rolling(window=period).mean()
    loss = (-delta.where(delta < 0, 0)).rolling(window=period).mean()

    # Division durch Null vermeiden (wenn loss = 0)
    rs = np.where(loss == 0, np.inf, gain / loss)
    
    # RSI Formel: 100 - (100 / (1 + RS))
    rsi = 100 - (100 / (1 + rs))
    return rsi


def prepare_training_data(
    raw_file_path: Path,
    horizon: int = DEFAULT_LABEL_HORIZON,
    min_move_pct: float = DEFAULT_MIN_MOVE_PCT
) -> Path:
    """
    Verarbeitet Rohdaten (OHLCV) zu einem Feature-Set für das Training.

    Berechnete Features:
    1. close: Aktueller Preis
    2. sma: Simple Moving Average (Gleitender Durchschnitt)
    3. rsi: Relative Strength Index (Momentum)
    4. close_minus_sma: Abstand zum Durchschnitt (Trend-Indikator)
    5. return_1: Rendite der letzten Kerze
    6. return_3: Rendite der letzten 3 Kerzen
    7. volatility_6: Volatilität (Standardabweichung der Renditen)
    8. close_minus_ema: Abstand zum Exponential Moving Average

    Labeling-Logik:
    - STEIGT (1), wenn Preis in 'horizon' Kerzen um >= 'min_move_pct' steigt.
    - FAELLT (0), wenn Preis in 'horizon' Kerzen um >= 'min_move_pct' fällt.
    - NEUTRAL (NaN), wenn Bewegung innerhalb der Schwelle bleibt (wird gedroppt).

    Args:
        raw_file_path: Pfad zur Rohdaten-CSV (von Binance).
        horizon: Vorhersage-Horizont.
        min_move_pct: Mindestbewegungsschwelle.

    Returns:
        Path: Pfad zur erstellten training_data.csv.
    """
    print(f"Verarbeite Rohdaten: {raw_file_path}")
    df = pd.read_csv(raw_file_path)

    # --- Feature Engineering ---
    # Gleitende Durchschnitte
    df["sma"] = df["close"].rolling(window=SMA_PERIOD).mean()
    df["ema"] = df["close"].ewm(span=EMA_PERIOD, adjust=False).mean()
    
    # Oszillatoren & Abstände
    df["rsi"] = calculate_rsi(df["close"], period=RSI_PERIOD)
    df["close_minus_sma"] = df["close"] - df["sma"]
    df["close_minus_ema"] = df["close"] - df["ema"]
    
    # Renditen (Returns)
    df["return_1"] = df["close"].pct_change(1)
    df["return_3"] = df["close"].pct_change(3)
    
    # Risiko-Indikator (Volatilität)
    df["volatility_6"] = df["return_1"].rolling(window=VOLATILITY_PERIOD).std()

    # --- Ziel-Label-Generierung (Target) ---
    # Wir schauen 'horizon' Schritte in die Zukunft
    future_close = df["close"].shift(-horizon)
    # Prozentuale Änderung zur Zukunft berechnen
    df["future_return_pct"] = ((future_close - df["close"]) / df["close"]) * 100.0

    # Labels vergeben basierend auf der min_move_pct Schwelle
    # 1.0 = UP, 0.0 = DOWN, nan = Seitwärts/Rauschen
    df["target"] = np.where(
        df["future_return_pct"] >= min_move_pct,
        1.0,
        np.where(df["future_return_pct"] <= -min_move_pct, 0.0, np.nan)
    )
    df["label"] = df["target"].map({1.0: "STEIGT", 0.0: "FAELLT"})

    # --- Datenbereinigung ---
    # Zeilen mit NaN (Warmup-Phase der Indikatoren oder neutrale Labels) entfernen
    rows_before = len(df)
    df.dropna(subset=["label", "sma", "rsi", "volatility_6"], inplace=True)
    rows_after = len(df)

    # Nur die für das Modell relevanten Spalten behalten
    final_columns = [
        "close", "sma", "rsi", "close_minus_sma", 
        "return_1", "return_3", "volatility_6", "close_minus_ema",
        "label"
    ]
    final_df = df[final_columns]

    # Speichern der bereinigten Daten
    output_path = raw_file_path.parent / "training_data.csv"
    final_df.to_csv(output_path, index=False)

    print(f"Training-Features generiert: {len(final_df)} Datensätze.")
    print(f"Gefiltert: {rows_before - rows_after} Zeilen (Rauschen/Warmup).")
    
    return output_path
