"""
Modul zum Trainieren des Machine-Learning-Modells für den KITradingBot.

Dieses Skript lädt vorbereitete Trainingsdaten, trainiert ein Klassifikationsmodell
(standardmäßig RandomForestClassifier, mit Fallback auf HeuristicClassifier)
und speichert das trainierte Modell als `model.pkl`.
"""

from pathlib import Path
import csv
import joblib
from simple_model import HeuristicClassifier
from typing import List, Any

# Versuch, Scikit-learn zu importieren. Wenn nicht verfügbar, wird ein Fallback verwendet.
try:
    from sklearn.ensemble import RandomForestClassifier
    from sklearn.metrics import classification_report
    from sklearn.model_selection import train_test_split
    SKLEARN_AVAILABLE = True
except ImportError:
    # Wenn Scikit-learn nicht installiert ist, wird der HeuristicClassifier verwendet.
    SKLEARN_AVAILABLE = False
    print("Warnung: Scikit-learn nicht gefunden. Verwende HeuristicClassifier als Fallback.")

# --- Globale Konfiguration ---
# Die Reihenfolge der Features, die das Modell erwartet. Muss konsistent sein mit data_processor.py und Java FeaturePipeline.
FEATURE_ORDER: List[str] = ["close", "sma", "rsi", "close_minus_sma"]
# Pfad, unter dem das trainierte Modell gespeichert wird.
MODEL_PATH: Path = Path(__file__).resolve().parent / "model.pkl"
# Standardpfad zu den vorbereiteten Trainingsdaten.
DEFAULT_DATA_PATH: Path = Path(__file__).resolve().parent / "data" / "training_data.csv"

def train(data_path: Path) -> None:
    """
    Trainiert das ML-Modell basierend auf den aufbereiteten CSV-Daten.

    Lädt Daten, bereitet sie für das Training vor, trainiert entweder einen
    RandomForestClassifier (falls Scikit-learn verfügbar) oder einen
    HeuristicClassifier und speichert das Modell.

    Args:
        data_path (Path): Der Pfad zur CSV-Datei mit den vorbereiteten Trainingsdaten.

    Raises:
        ValueError: Wenn die Trainingsdaten fehlende Spalten enthalten
                    oder ungültige Labels gefunden werden.
    """
    print(f"Starte Modelltraining mit Daten von: {data_path}")

    # Daten aus der CSV-Datei laden
    x: List[List[float]] = []  # Features
    y: List[int] = []          # Labels (0 für FÄLLT, 1 für STEIGT)

    with data_path.open("r", encoding="utf-8", newline="") as file:
        reader = csv.DictReader(file)

        # Überprüfen, ob alle benötigten Spalten vorhanden sind
        required_columns = FEATURE_ORDER + ["label"]
        missing = [col for col in required_columns if col not in reader.fieldnames]
        if missing:
            raise ValueError(f"Fehlende Spalten in der CSV-Datei: {missing}. Erwartet: {required_columns}")

        # Iterieren über die Zeilen und Features/Labels extrahieren
        for row in reader:
            # Features in der definierten Reihenfolge extrahieren und zu Float konvertieren
            x.append([float(row[name]) for name in FEATURE_ORDER])

            # Label extrahieren und auf Großbuchstaben konvertieren
            label = row["label"].strip().upper()

            # Labels in numerische Werte umwandeln (1 für STEIGT, 0 für FÄLLT)
            if label in {"STEIGT", "UP"}:
                y.append(1)
            elif label in {"FÄLLT", "DOWN"}:
                y.append(0)
            else:
                raise ValueError(f"Ungültiges Label '{label}' gefunden. Erwartet: 'STEIGT' oder 'FÄLLT'.")

    # Sicherstellen, dass Daten zum Trainieren vorhanden sind
    if not x:
        raise ValueError("Keine Daten zum Trainieren gefunden. Bitte Marktdaten aktualisieren.")

    # --- Modelltraining ---
    if SKLEARN_AVAILABLE:
        print("Scikit-learn verfügbar: Trainiere RandomForestClassifier...")
        # Aufteilung der Daten in Trainings- und Testsets
        x_train, x_test, y_train, y_test = train_test_split(
            x, y, test_size=0.2, random_state=42, stratify=y # stratify=y für ausgewogene Klassenverteilung
        )

        # Initialisierung und Training des RandomForestClassifier
        model = RandomForestClassifier(
            n_estimators=250,          # Anzahl der Bäume im Wald
            max_depth=8,               # Maximale Tiefe der Bäume
            random_state=42,           # Für Reproduzierbarkeit
            class_weight="balanced_subsample", # Behandelt unausgewogene Klassen
        )
        model.fit(x_train, y_train)

        # Evaluierung des Modells auf dem Testset
        pred = model.predict(x_test)
        print("\n--- Klassifikationsbericht (Testdaten) ---")
        print(classification_report(y_test, pred, target_names=["FÄLLT", "STEIGT"]))
        print("------------------------------------------")
    else:
        print("Scikit-learn nicht verfügbar: Nutze HeuristicClassifier Fallback.")
        # Wenn Scikit-learn nicht installiert ist, wird der einfache heuristische Klassifikator verwendet.
        model = HeuristicClassifier()
        model.fit(x, y) # Die fit-Methode des HeuristicClassifiers tut nichts, ist aber für die Schnittstelle da.

    # --- Modell speichern ---
    # Speichert das trainierte Modell und die Feature-Reihenfolge als Artefakt.
    # joblib ist effizienter für große NumPy-Arrays als pickle.
    artifact: Dict[str, Any] = {"model": model, "feature_order": FEATURE_ORDER}
    joblib.dump(artifact, MODEL_PATH)
    print(f"Modell erfolgreich gespeichert unter {MODEL_PATH}")

if __name__ == "__main__":
    # Führt das Training aus, wenn das Skript direkt gestartet wird.
    # Stellt sicher, dass der Datenpfad existiert.
    if not DEFAULT_DATA_PATH.exists():
        print(f"Fehler: Trainingsdaten nicht gefunden unter {DEFAULT_DATA_PATH}")
        print("Bitte zuerst 'Marktdaten aktualisieren' im Frontend klicken oder downloader.py/data_processor.py ausführen.")
    else:
        train(DEFAULT_DATA_PATH)
