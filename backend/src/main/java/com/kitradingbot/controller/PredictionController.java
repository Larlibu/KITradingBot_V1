package com.kitradingbot.controller;

import com.kitradingbot.integration.MlServiceClient;
import com.kitradingbot.model.MlPredictionRequest;
import com.kitradingbot.model.MlPredictionResponse;
import com.kitradingbot.service.PredictionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST-Controller für KI-Vorhersagen, Modell-Management und Daten-Akquise.
 * Dieser Controller dient als Schnittstelle zum externen ML-Service (Python).
 */
@RestController
@RequestMapping("/api/predict")
public class PredictionController {
    private final PredictionService predictionService;
    private final MlServiceClient mlServiceClient;

    /**
     * Konstruktor für den PredictionController.
     * Spring injiziert automatisch die benötigten Abhängigkeiten.
     *
     * @param predictionService Der Service für die KI-Vorhersagen und das Training.
     * @param mlServiceClient   Der Client für die direkte Kommunikation mit dem ML-Service.
     */
    public PredictionController(PredictionService predictionService, MlServiceClient mlServiceClient) {
        this.predictionService = predictionService;
        this.mlServiceClient = mlServiceClient;
    }

    /**
     * Fordert eine Vorhersage vom ML-Service an.
     * Akzeptiert einen Feature-Vektor und gibt die Vorhersage des Modells zurück.
     *
     * @param request Ein {@link MlPredictionRequest} Objekt, das den Feature-Vektor enthält.
     * @return Ein {@link MlPredictionResponse} Objekt mit dem vorhergesagten Label und der Konfidenz.
     */
    @PostMapping
    public MlPredictionResponse predict(@Valid @RequestBody MlPredictionRequest request) {
        // Konvertiert die Liste von Double-Features in ein double-Array für den PredictionService.
        double[] features = request.features().stream().mapToDouble(Double::doubleValue).toArray();
        return predictionService.predict(features);
    }

    /**
     * Löst das Training des ML-Modells im externen ML-Service aus.
     * Das Training läuft asynchron im ML-Service.
     *
     * @return Eine {@link ResponseEntity} mit einer Erfolgsmeldung.
     */
    @PostMapping("/train")
    public ResponseEntity<Map<String, String>> train() {
        predictionService.triggerMlTraining();
        return ResponseEntity.ok(Map.of("message", "Modell-Training wurde gestartet."));
    }

    /**
     * Ruft den aktuellen Status des Modelltrainings vom ML-Service ab.
     *
     * @return Eine Map, die den Trainingsstatus enthält (z.B. "idle", "running", "success", "error").
     */
    @GetMapping("/train/status")
    public Map<String, Object> getTrainingStatus() {
        return mlServiceClient.getTrainingStatus();
    }

    /**
     * Löst den Download und die Vorbereitung neuer Marktdaten im externen ML-Service aus.
     * Der Daten-Download läuft asynchron im ML-Service.
     *
     * @return Eine {@link ResponseEntity} mit einer Erfolgsmeldung.
     */
    @PostMapping("/data/update")
    public ResponseEntity<Map<String, String>> triggerDataUpdate() {
        mlServiceClient.triggerDataUpdate();
        return ResponseEntity.ok(Map.of("message", "Marktdaten-Download wurde gestartet."));
    }

    /**
     * Ruft den aktuellen Status des Marktdaten-Downloads und der Vorbereitung vom ML-Service ab.
     *
     * @return Eine Map, die den Daten-Status enthält (z.B. "idle", "running", "success", "error").
     */
    @GetMapping("/data/status")
    public Map<String, Object> getDataStatus() {
        return mlServiceClient.getDataStatus();
    }
}
