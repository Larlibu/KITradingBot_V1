"""
FastAPI-basierter Machine Learning Service für den KITradingBot.

Dieser Service stellt REST-Endpunkte für folgende Funktionen bereit:
- Vorhersagen basierend auf Feature-Vektoren (einzeln und im Batch).
- Auslösen und Überwachen des Modelltrainings.
- Auslösen und Überwachen des Marktdaten-Downloads und der Vorbereitung.
- Bereitstellung von historischen Marktdaten für das Java-Backend.
"""

from pathlib import Path
from typing import List, Dict, Any
import threading
import time
import pandas as pd

import joblib
from fastapi import FastAPI, HTTPException, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

# Import der Trainingslogik und Datenpfade
try:
    from train import train, DEFAULT_DATA_PATH
except ImportError:
    # Fallback, falls train.py nicht verfügbar ist (z.B. bei reiner Inferenz-Bereitstellung)
    train = None
    DEFAULT_DATA_PATH = None

# Import der Downloader- und Datenverarbeitungslogik
from downloader import fetch_binance_data, SYMBOL, DATA_DIR
from data_processor import prepare_training_data

# --- Pydantic Models für Request- und Response-Validierung ---

class PredictRequest(BaseModel):
    """Modell für eine einzelne Vorhersageanfrage."""
    features: List[float] = Field(min_length=1, description="Liste der numerischen Features für die Vorhersage.")

class BatchPredictRequest(BaseModel):
    """Modell für eine Batch-Vorhersageanfrage."""
    batch: List[List[float]] = Field(description="Liste von Feature-Vektoren für Batch-Vorhersagen.")

class PredictResponse(BaseModel):
    """Modell für die Antwort einer einzelnen Vorhersage."""
    label: str = Field(description="Das vorhergesagte Label (z.B. 'STEIGT', 'FÄLLT').")
    confidence: float = Field(description="Die Konfidenz des Modells für die Vorhersage (0.0 bis 1.0).")

class BatchPredictResponse(BaseModel):
    """Modell für die Antwort einer Batch-Vorhersage."""
    predictions: List[PredictResponse] = Field(description="Liste der einzelnen Vorhersage-Antworten.")

# --- Globale Konfiguration und Status ---

# Pfad zum gespeicherten ML-Modell (model.pkl)
MODEL_PATH = Path(__file__).resolve().parent / "model.pkl"
# Initialisierung der FastAPI-Anwendung
app = FastAPI(title="KI TradingBot ML Service")

# Globale Variablen zur Speicherung des Trainings- und Download-Status
# Diese werden von den Hintergrundaufgaben aktualisiert und über API-Endpunkte abgefragt.
training_info: Dict[str, Any] = {"status": "idle", "last_run": None, "error_message": None}
download_info: Dict[str, Any] = {"status": "idle", "last_update": None, "error_message": None}

# Konfiguration der CORS-Middleware, um Cross-Origin-Anfragen vom Frontend zu erlauben
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Erlaubt Anfragen von jeder Origin (für Entwicklung)
    allow_methods=["*"],  # Erlaubt alle HTTP-Methoden
    allow_headers=["*"],  # Erlaubt alle Header
)

# --- API Endpunkte ---

@app.get("/health", summary="Statusprüfung des ML-Services")
def health() -> Dict[str, str]:
    """
    Überprüft den Gesundheitszustand des ML-Services.
    Gibt einen einfachen Status zurück, um die Erreichbarkeit zu signalisieren.
    """
    return {"status": "ok"}

@app.get("/data/candles", summary="Historische Marktdaten abrufen")
async def get_candles(limit: int = 500) -> List[Dict[str, Any]]:
    """
    Gibt die zuletzt heruntergeladenen historischen Marktdaten (Candles) zurück.
    Diese Daten werden aus lokalen CSV-Dateien gelesen.

    Args:
        limit (int): Die maximale Anzahl der zurückzugebenden Kerzen.

    Returns:
        List[Dict[str, Any]]: Eine Liste von Candle-Objekten im Dictionary-Format.

    Raises:
        HTTPException: Wenn keine Marktdaten-Dateien gefunden werden.
    """
    raw_file_1h = DATA_DIR / "raw_1h.csv"
    raw_file_1m = DATA_DIR / "raw_1m.csv"

    # Versucht zuerst 1h-Daten, dann 1m-Daten zu laden
    if raw_file_1h.exists():
        raw_file = raw_file_1h
    elif raw_file_1m.exists():
        raw_file = raw_file_1m
    else:
        raise HTTPException(status_code=404, detail="Keine Marktdaten gefunden. Bitte zuerst 'Marktdaten aktualisieren' klicken.")

    # Lädt die Daten und gibt die letzten 'limit' Kerzen zurück
    df = pd.read_csv(raw_file)
    df = df.tail(limit)

    # Konvertiert DataFrame in eine Liste von Dictionaries, die dem Java Candle-Modell entsprechen
    candles = []
    for _, row in df.iterrows():
        candles.append({
            "timestamp": row["open_time"],
            "open": float(row["open"]),
            "high": float(row["high"]),
            "low": float(row["low"]),
            "close": float(row["close"]),
            "volume": float(row["volume"])
        })
    return candles


@app.post("/predict", response_model=PredictResponse, summary="Einzelne Vorhersage anfordern")
def predict(request: PredictRequest) -> PredictResponse:
    """
    Führt eine einzelne Vorhersage mit dem geladenen ML-Modell durch.

    Args:
        request (PredictRequest): Das Request-Objekt mit dem Feature-Vektor.

    Returns:
        PredictResponse: Die Vorhersage des Modells (Label und Konfidenz).

    Raises:
        HTTPException: Wenn das ML-Modell nicht geladen werden kann oder ein Fehler auftritt.
    """
    try:
        # Lädt das Modell bei jeder Anfrage (kann optimiert werden durch Caching in einer globalen Variable)
        artifact = joblib.load(MODEL_PATH)
        model = artifact["model"]

        # Bereitet den Feature-Vektor für die Vorhersage vor
        x = [request.features]

        # Führt die Vorhersage und Wahrscheinlichkeitsberechnung durch
        pred_raw = model.predict(x)[0]
        proba = model.predict_proba(x)[0]

        # Konvertiert die rohe Vorhersage in ein lesbares Label
        pred_label = int(pred_raw)
        label = "STEIGT" if pred_label == 1 else "FÄLLT"
        confidence = float(proba[pred_label])

        return PredictResponse(label=label, confidence=confidence)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"KI Modell nicht bereit oder Fehler bei Vorhersage: {exc}")

@app.post("/predict/batch", response_model=BatchPredictResponse, summary="Batch-Vorhersage anfordern")
def predict_batch(request: BatchPredictRequest) -> BatchPredictResponse:
    """
    Führt Batch-Vorhersagen mit dem geladenen ML-Modell durch.
    Optimiert für die schnelle Verarbeitung mehrerer Feature-Vektoren, z.B. für Backtesting.

    Args:
        request (BatchPredictRequest): Das Request-Objekt mit einer Liste von Feature-Vektoren.

    Returns:
        BatchPredictResponse: Eine Liste von Vorhersage-Antworten.

    Raises:
        HTTPException: Wenn das ML-Modell nicht geladen werden kann oder ein Fehler auftritt.
    """
    try:
        # Lädt das Modell bei jeder Anfrage (kann optimiert werden durch Caching)
        artifact = joblib.load(MODEL_PATH)
        model = artifact["model"]

        # Führt alle Vorhersagen in einem Rutsch durch (sehr effizient in Scikit-Learn)
        preds_raw = model.predict(request.batch)
        probas = model.predict_proba(request.batch)

        results: List[PredictResponse] = []
        for i, pred_raw in enumerate(preds_raw):
            pred_label = int(pred_raw)
            label = "STEIGT" if pred_label == 1 else "FÄLLT"
            confidence = float(probas[i][pred_label])
            results.append(PredictResponse(label=label, confidence=confidence))

        return BatchPredictResponse(predictions=results)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"Batch-KI Fehler: {exc}")

# --- TRAINING LOGIK ---

def run_training_task(path: Path):
    """
    Hintergrundaufgabe zum Auslösen des Modelltrainings.
    Aktualisiert den globalen `training_info`-Status.

    Args:
        path (Path): Der Pfad zu den Trainingsdaten.
    """
    global training_info
    training_info["status"] = "running"
    training_info["error_message"] = None
    try:
        if train: # Sicherstellen, dass die train-Funktion importiert wurde
            train(path)
            training_info["status"] = "success"
            training_info["last_run"] = time.strftime("%Y-%m-%d %H:%M:%S")
        else:
            raise RuntimeError("Trainingsfunktion nicht verfügbar. train.py konnte nicht importiert werden.")
    except Exception as e:
        training_info["status"] = "error"
        training_info["error_message"] = str(e)

@app.post("/train", summary="Modelltraining auslösen")
async def trigger_training(background_tasks: BackgroundTasks) -> Dict[str, str]:
    """
    Löst das Training des ML-Modells als Hintergrundaufgabe aus.
    """
    background_tasks.add_task(run_training_task, DEFAULT_DATA_PATH)
    return {"message": "Training gestartet."}

@app.get("/train/status", summary="Trainingsstatus abrufen")
async def get_training_status() -> Dict[str, Any]:
    """
    Gibt den aktuellen Status des Modelltrainings zurück.
    """
    return training_info

# --- DATEN-DOWNLOAD LOGIK ---

def run_data_update_task():
    """
    Hintergrundaufgabe zum Herunterladen und Vorbereiten neuer Marktdaten.
    Aktualisiert den globalen `download_info`-Status.
    """
    global download_info
    download_info["status"] = "running"
    download_info["error_message"] = None
    try:
        # Lädt Daten von Binance (z.B. 730 Stunden = ~1 Monat)
        raw_file = fetch_binance_data(SYMBOL, "1h", 730)
        # Bereitet die heruntergeladenen Daten für das Training auf
        prepare_training_data(raw_file)
        download_info["status"] = "success"
        download_info["last_update"] = time.strftime("%Y-%m-%d %H:%M:%S")
    except Exception as e:
        download_info["status"] = "error"
        download_info["error_message"] = str(e)

@app.post("/data/update", summary="Marktdaten-Download auslösen")
async def trigger_data_update(background_tasks: BackgroundTasks) -> Dict[str, str]:
    """
    Löst den Download und die Vorbereitung neuer Marktdaten als Hintergrundaufgabe aus.
    """
    background_tasks.add_task(run_data_update_task)
    return {"message": "Daten-Download gestartet."}

@app.get("/data/status", summary="Marktdaten-Downloadstatus abrufen")
async def get_data_status() -> Dict[str, Any]:
    """
    Gibt den aktuellen Status des Marktdaten-Downloads und der Vorbereitung zurück.
    """
    return download_info
