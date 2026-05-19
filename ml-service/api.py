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
    """Modell für eine einzelne Vorhersage anfrage."""
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

# Globaler Cache für das Modell-Artefakt
_MODEL_CACHE: Dict[str, Any] = {}

def get_model_artifact() -> Dict[str, Any]:
    """
    Lädt das Modell-Artefakt einmalig und speichert es im Cache.
    """
    if not _MODEL_CACHE:
        if not MODEL_PATH.exists():
            raise RuntimeError(f"Modell-Datei nicht gefunden unter {MODEL_PATH}. Bitte zuerst Training ausführen.")
        _MODEL_CACHE["artifact"] = joblib.load(MODEL_PATH)
    return _MODEL_CACHE["artifact"]

@app.on_event("startup")
def startup_event():
    """Wird beim Starten der API aufgerufen."""
    try:
        if MODEL_PATH.exists():
            get_model_artifact()
            print("ML-Modell erfolgreich beim Start geladen.")
    except Exception as e:
        print(f"Warnung: Modell konnte beim Start nicht geladen werden: {e}")

def clear_model_cache():
    """Löscht den Modell-Cache (z.B. nach einem neuen Training)."""
    _MODEL_CACHE.clear()

# Globale Variablen zur Speicherung des Trainings- und Download-Status
training_info: Dict[str, Any] = {"status": "idle", "last_run": None, "error_message": None}
download_info: Dict[str, Any] = {"status": "idle", "last_update": None, "error_message": None}

# Konfiguration der CORS-Middleware, um Cross-Origin-Anfragen vom Frontend zu erlauben
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# --- API Endpunkte ---

@app.get("/health", summary="Statusprüfung des ML-Services")
def health() -> Dict[str, str]:
    """Überprüft den Gesundheitszustand des ML-Services."""
    return {"status": "ok"}

@app.get("/data/candles", summary="Historische Marktdaten abrufen")
async def get_candles(limit: int = 500) -> List[Dict[str, Any]]:
    """Gibt die zuletzt heruntergeladenen historischen Marktdaten (Candles) zurück."""
    raw_file_1h = DATA_DIR / "raw_1h.csv"
    raw_file_1m = DATA_DIR / "raw_1m.csv"

    if raw_file_1h.exists():
        raw_file = raw_file_1h
    elif raw_file_1m.exists():
        raw_file = raw_file_1m
    else:
        raise HTTPException(status_code=404, detail="Keine Marktdaten gefunden.")

    df = pd.read_csv(raw_file)
    df = df.tail(limit)

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
    """Führt eine einzelne Vorhersage mit dem geladenen ML-Modell durch."""
    try:
        artifact = get_model_artifact()
        model = artifact["model"]
        x = [request.features]
        pred_raw = model.predict(x)[0]
        proba = model.predict_proba(x)[0]
        pred_label = int(pred_raw)
        label = "STEIGT" if pred_label == 1 else "FAELLT"
        confidence = float(proba[pred_label])
        return PredictResponse(label=label, confidence=confidence)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"Fehler bei Vorhersage: {exc}")

@app.post("/predict/batch", response_model=BatchPredictResponse, summary="Batch-Vorhersage anfordern")
def predict_batch(request: BatchPredictRequest) -> BatchPredictResponse:
    """Führt Batch-Vorhersagen mit dem geladenen ML-Modell durch."""
    try:
        artifact = get_model_artifact()
        model = artifact["model"]
        preds_raw = model.predict(request.batch)
        probas = model.predict_proba(request.batch)
        results = []
        for i, pred_raw in enumerate(preds_raw):
            pred_label = int(pred_raw)
            label = "STEIGT" if pred_label == 1 else "FAELLT"
            confidence = float(probas[i][pred_label])
            results.append(PredictResponse(label=label, confidence=confidence))
        return BatchPredictResponse(predictions=results)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"Batch-Fehler: {exc}")

# --- TRAINING LOGIK ---

def run_training_task(path: Path):
    global training_info
    training_info["status"] = "running"
    training_info["error_message"] = None
    try:
        if train:
            train(path)
            clear_model_cache()
            training_info["status"] = "success"
            training_info["last_run"] = time.strftime("%Y-%m-%d %H:%M:%S")
        else:
            raise RuntimeError("Trainingsfunktion nicht verfügbar.")
    except Exception as e:
        training_info["status"] = "error"
        training_info["error_message"] = str(e)

@app.post("/train", summary="Modelltraining auslösen")
async def trigger_training(background_tasks: BackgroundTasks) -> Dict[str, str]:
    background_tasks.add_task(run_training_task, DEFAULT_DATA_PATH)
    return {"message": "Training gestartet."}

@app.get("/train/status", summary="Trainingsstatus abrufen")
async def get_training_status() -> Dict[str, Any]:
    return training_info

# --- DATEN-DOWNLOAD LOGIK ---

def run_data_update_task():
    global download_info
    download_info["status"] = "running"
    download_info["error_message"] = None
    try:
        raw_file = fetch_binance_data(SYMBOL, "1h", 730)
        prepare_training_data(raw_file)
        download_info["status"] = "success"
        download_info["last_update"] = time.strftime("%Y-%m-%d %H:%M:%S")
    except Exception as e:
        download_info["status"] = "error"
        download_info["error_message"] = str(e)

@app.post("/data/update", summary="Marktdaten-Download auslösen")
async def trigger_data_update(background_tasks: BackgroundTasks) -> Dict[str, str]:
    background_tasks.add_task(run_data_update_task)
    return {"message": "Daten-Download gestartet."}

@app.get("/data/status", summary="Marktdaten-Downloadstatus abrufen")
async def get_data_status() -> Dict[str, Any]:
    return download_info
