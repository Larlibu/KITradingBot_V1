"""
Modul zum Trainieren des Machine-Learning-Modells fuer den KITradingBot.

Dieses Skript laedt vorbereitete Trainingsdaten, trainiert ein Klassifikationsmodell
(standardmaessig RandomForestClassifier mit Kalibrierung) und speichert das
trainierte Modell sowie Metadaten in einer .pkl Datei.
"""

import csv
from pathlib import Path
from typing import Any, Dict, List, Tuple

import joblib

# Fallback-Modell falls sklearn nicht installiert ist
from simple_model import HeuristicClassifier

# Import von Standardwerten für die Dokumentation im Modell-Artefakt
try:
    from data_processor import DEFAULT_LABEL_HORIZON, DEFAULT_MIN_MOVE_PCT
except ImportError:
    DEFAULT_LABEL_HORIZON = 3
    DEFAULT_MIN_MOVE_PCT = 0.20

# Versuch, Scikit-learn zu importieren.
try:
    from sklearn.calibration import CalibratedClassifierCV
    from sklearn.ensemble import RandomForestClassifier
    from sklearn.metrics import classification_report, brier_score_loss, log_loss
    from sklearn.model_selection import TimeSeriesSplit

    SKLEARN_AVAILABLE = True
except ImportError:
    SKLEARN_AVAILABLE = False
    print("Warnung: Scikit-learn nicht gefunden. Verwende HeuristicClassifier als Fallback.")

# --- Globale Konfiguration ---
# Die Reihenfolge MUSS identisch mit der FeaturePipeline im Java-Backend sein!
FEATURE_ORDER: List[str] = [
    "close",
    "sma",
    "rsi",
    "close_minus_sma",
    "return_1",
    "return_3",
    "volatility_6",
    "close_minus_ema"
]

# Pfade für Ein- und Ausgabe
MODEL_PATH: Path = Path(__file__).resolve().parent / "model.pkl"
DEFAULT_DATA_PATH: Path = Path(__file__).resolve().parent / "data" / "training_data.csv"


def _load_dataset(data_path: Path) -> Tuple[List[List[float]], List[int]]:
    """
    Lädt Features (X) und Labels (y) aus der vorbereiteten CSV-Datei.
    
    Args:
        data_path: Pfad zur CSV mit Spalten für Features und 'label'.
        
    Returns:
        Tuple aus Feature-Liste (X) und Label-Liste (y).
    """
    x: List[List[float]] = []
    y: List[int] = []

    if not data_path.exists():
        raise FileNotFoundError(f"Trainingsdaten nicht gefunden: {data_path}")

    with data_path.open("r", encoding="utf-8", newline="") as file:
        reader = csv.DictReader(file)
        
        # Validierung der Spaltenstruktur
        required_columns = FEATURE_ORDER + ["label"]
        missing = [col for col in required_columns if col not in reader.fieldnames]
        if missing:
            raise ValueError(f"Fehlende Spalten in CSV: {missing}. Erwartet: {required_columns}")

        for row in reader:
            # Features extrahieren gemäß der festgelegten Reihenfolge
            x.append([float(row[name]) for name in FEATURE_ORDER])
            
            # Label in numerisches Format wandeln (1 = STEIGT, 0 = FÄLLT)
            label = row["label"].strip().upper()
            if label in {"STEIGT", "UP"}:
                y.append(1)
            elif label in {"FÄLLT", "FAELLT", "DOWN"}:
                y.append(0)
            else:
                # Unbekannte Labels überspringen oder Fehler werfen
                continue

    if not x:
        raise ValueError("Keine gültigen Trainingsdaten nach dem Laden gefunden.")
    
    return x, y


def _build_rf_model() -> "RandomForestClassifier":
    """
    Konfiguriert den Basis-RandomForest-Klassifikator.
    Parameter sind auf Stabilität und Vermeidung von Overfitting optimiert.
    """
    return RandomForestClassifier(
        n_estimators=300,            # Anzahl der Bäume
        max_depth=10,                # Begrenzung der Tiefe gegen Overfitting
        random_state=42,             # Reproduzierbarkeit
        class_weight="balanced_subsample", # Umgang mit ungleichen Klassenverteilungen
        min_samples_leaf=2           # Mindestanzahl an Proben pro Blatt
    )


def _build_calibrated_model(sample_count: int):
    """
    Erzeugt ein kalibriertes Modell für verlässliche Wahrscheinlichkeitsvorhersagen.
    Wählt die Kalibrierungsmethode basierend auf der Datensatzgröße.
    
    Args:
        sample_count: Anzahl der verfügbaren Trainingsbeispiele.
    """
    base_model = _build_rf_model()
    
    # Bei sehr wenigen Daten macht Kalibrierung keinen Sinn
    if sample_count < 400:
        return base_model

    # Isotonic ist genauer bei vielen Daten, Sigmoid (Platt Scaling) stabiler bei weniger.
    method = "isotonic" if sample_count > 1000 else "sigmoid"

    # Kreuzvalidierung für die Kalibrierung (TimeSeriesSplit da Zeitreihendaten)
    calibration_splits = min(5, max(2, sample_count // 1000))
    
    try:
        return CalibratedClassifierCV(
            estimator=base_model,
            method=method,
            cv=TimeSeriesSplit(n_splits=calibration_splits)
        )
    except TypeError:
        # Fallback für ältere sklearn Versionen (base_estimator statt estimator)
        return CalibratedClassifierCV(
            base_estimator=base_model,
            method=method,
            cv=TimeSeriesSplit(n_splits=calibration_splits)
        )


def _determine_splits(sample_count: int) -> int:
    """Berechnet die Anzahl der Splits für die Zeitreihen-Kreuzvalidierung."""
    if sample_count < 40:
        return 0
    return min(5, max(2, sample_count // 120))


def _evaluate_walk_forward(x: List[List[float]], y: List[int]) -> None:
    """
    Führt eine Walk-Forward-Evaluierung durch (Backtest für ML).
    Dies simuliert den Einsatz in der Realität: Training auf Vergangenheit, Test auf Zukunft.
    """
    n_splits = _determine_splits(len(x))
    if n_splits < 2:
        print("Hinweis: Zu wenige Daten fuer TimeSeriesSplit-Evaluierung.")
        return

    tscv = TimeSeriesSplit(n_splits=n_splits)
    y_true: List[int] = []
    y_pred: List[int] = []
    y_probs: List[float] = []

    print(f"Starte Walk-Forward-Evaluierung mit {n_splits} Splits...")
    
    for fold_idx, (train_idx, test_idx) in enumerate(tscv.split(x), start=1):
        # Pro Fold ein frisches kalibriertes Modell trainieren
        fold_model = _build_calibrated_model(len(train_idx))
        
        # Daten für diesen Fold aufteilen
        x_train = [x[i] for i in train_idx]
        y_train = [y[i] for i in train_idx]
        x_test = [x[i] for i in test_idx]
        y_test = [y[i] for i in test_idx]

        # Training
        fold_model.fit(x_train, y_train)

        # Vorhersage (Klassen und Wahrscheinlichkeiten)
        fold_pred = fold_model.predict(x_test)
        fold_proba = fold_model.predict_proba(x_test)[:, 1] # Wahrscheinlichkeit für UP

        y_true.extend(y_test)
        y_pred.extend(fold_pred)
        y_probs.extend(fold_proba)

        print(f"Fold {fold_idx}/{n_splits} abgeschlossen.")

    # Ergebnisse ausgeben
    print("\n--- Metriken (Walk-Forward-Validation) ---")
    print(classification_report(y_true, y_pred, target_names=["FAELLT", "STEIGT"], zero_division=0))
    
    # Konfidenz-Qualität messen
    brier = brier_score_loss(y_true, y_probs)
    ll = log_loss(y_true, y_probs)
    
    print(f"Brier Score (Konfidenz-Fehler): {brier:.4f} (0.0 ist perfekt)")
    print(f"Log Loss:                      {ll:.4f}")
    print("--------------------------------------------")


def train(data_path: Path) -> None:
    """Hauptfunktion für den Trainingsprozess."""
    print(f"Starte Modelltraining. Datenquelle: {data_path}")
    
    # 1. Daten laden
    x, y = _load_dataset(data_path)

    # 2. Training und Evaluierung
    if SKLEARN_AVAILABLE:
        # Erst Qualität prüfen
        _evaluate_walk_forward(x, y)

        # Dann finales Modell auf ALLEN Daten trainieren
        print("Trainiere finales Modell...")
        model = _build_calibrated_model(len(x))
        model.fit(x, y)
    else:
        # Fallback auf Heuristik
        print("Nutze HeuristicClassifier als Fallback.")
        model = HeuristicClassifier()
        model.fit(x, y)

    # 3. Artefakt speichern
    artifact: Dict[str, Any] = {
        "model": model,
        "feature_order": FEATURE_ORDER,
        "labeling_info": {
            "horizon": DEFAULT_LABEL_HORIZON,
            "min_move_pct": DEFAULT_MIN_MOVE_PCT
        }
    }
    joblib.dump(artifact, MODEL_PATH)
    print(f"Modell erfolgreich gespeichert: {MODEL_PATH}")


if __name__ == "__main__":
    train(DEFAULT_DATA_PATH)
