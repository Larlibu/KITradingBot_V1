"""
Modul zur Vorbereitung von Marktdaten für das Machine-Learning-Modell.

Enthält Funktionen zur Berechnung technischer Indikatoren und zur Generierung
von Ziel-Labels aus Rohdaten (Candlesticks).
"""

import pandas as pd
import numpy as np
from pathlib import Path
from typing import Union

def calculate_rsi(series: pd.Series, period: int = 14) -> pd.Series:
    """
    Berechnet den Relative Strength Index (RSI) für eine gegebene Preisreihe.

    Args:
        series (pd.Series): Eine Pandas Series, typischerweise die 'close'-Preise.
        period (int): Die Periode für die RSI-Berechnung.

    Returns:
        pd.Series: Eine Pandas Series mit den berechneten RSI-Werten.
    """
    delta = series.diff()
    # Gewinne: Positive Preisänderungen, Verluste: Negative Preisänderungen (als positive Werte)
    gain = (delta.where(delta > 0, 0)).rolling(window=period).mean()
    loss = (-delta.where(delta < 0, 0)).rolling(window=period).mean()

    # Vermeidung von Division durch Null bei 'loss'
    rs = np.where(loss == 0, np.inf, gain / loss)
    rsi = 100 - (100 / (1 + rs))
    return rsi

def prepare_training_data(raw_file_path: Path) -> Path:
    """
    Verarbeitet Rohdaten von Candlesticks zu einem DataFrame, der für das KI-Training bereit ist.
    Berechnet Features (Indikatoren) und generiert das Ziel-Label.

    Args:
        raw_file_path (Path): Der Pfad zur CSV-Datei mit den Rohdaten (Candlesticks).

    Returns:
        Path: Der Pfad zur erstellten CSV-Datei mit den vorbereiteten Trainingsdaten.
    """
    df = pd.read_csv(raw_file_path)

    # --- Feature-Berechnung ---
    # WICHTIG: Diese Features müssen 1:1 mit denen im Java-Backend (FeaturePipeline) übereinstimmen,
    # damit das ML-Modell im Live-Betrieb die gleichen Eingaben erhält wie im Training.
    df['sma'] = df['close'].rolling(window=14).mean()
    df['rsi'] = calculate_rsi(df['close'], period=14)
    df['close_minus_sma'] = df['close'] - df['sma']

    # --- Label-Generierung ---
    # Ziel: Vorhersage, ob der Preis in der NÄCHSTEN Periode steigen wird.
    # 'target' = 1 (STEIGT), wenn der Schlusskurs der nächsten Kerze höher ist als der aktuelle.
    # 'target' = 0 (FÄLLT), wenn der Schlusskurs der nächsten Kerze niedriger oder gleich ist.
    df['target'] = (df['close'].shift(-1) > df['close']).astype(int)
    df['label'] = df['target'].map({1: 'STEIGT', 0: 'FÄLLT'})

    # Bereinigen: Entfernt Zeilen mit NaN-Werten, die durch rolling-Berechnungen oder shift entstehen.
    # Diese Zeilen haben keine vollständigen Features oder Labels.
    df.dropna(inplace=True)

    # Nur die für das KI-Modell benötigten Spalten exportieren.
    # Die Reihenfolge der Features muss konsistent sein!
    final_df = df[['close', 'sma', 'rsi', 'close_minus_sma', 'label']]

    # Speichert die vorbereiteten Daten in einer neuen CSV-Datei.
    output_path = raw_file_path.parent / "training_data.csv"
    final_df.to_csv(output_path, index=False)
    print(f"Trainingsdaten erstellt: {output_path} ({len(final_df)} Zeilen)")
    return output_path
