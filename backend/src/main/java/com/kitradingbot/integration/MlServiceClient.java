package com.kitradingbot.integration;

import com.kitradingbot.config.TradingProperties;
import com.kitradingbot.model.MlPredictionRequest;
import com.kitradingbot.model.MlPredictionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Client für die Kommunikation mit dem externen Machine Learning Microservice (z.B. Python/FastAPI).
 * Dieser Client kapselt alle HTTP-Aufrufe an den ML-Service, um Vorhersagen anzufordern,
 * das Modell zu trainieren oder den Status von ML-Operationen abzufragen.
 */
@Component
public class MlServiceClient {
    private static final Logger log = LoggerFactory.getLogger(MlServiceClient.class);
    private final RestTemplate restTemplate;
    private final TradingProperties properties;

    /**
     * Konstruktor für den MlServiceClient.
     * Spring injiziert automatisch die benötigten Abhängigkeiten.
     *
     * @param restTemplate Ein vorkonfiguriertes {@link RestTemplate} für HTTP-Anfragen.
     * @param properties   Die globalen Trading-Konfigurationen, die die URL des ML-Services enthalten.
     */
    public MlServiceClient(RestTemplate restTemplate, TradingProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    /**
     * Sendet einen einzelnen Feature-Vektor an den ML-Service, um eine Vorhersage zu erhalten.
     *
     * @param features Ein `double`-Array, das den Feature-Vektor für die Vorhersage darstellt.
     * @return Ein {@link MlPredictionResponse}-Objekt mit dem vorhergesagten Label und der Konfidenz.
     * @throws RestClientException Wenn bei der Kommunikation mit dem ML-Service ein Fehler auftritt.
     */
    public MlPredictionResponse predict(double[] features) throws RestClientException {
        // Erstellt ein Request-Objekt, das die Features als Liste von Doubles enthält.
        MlPredictionRequest request = new MlPredictionRequest(
                java.util.Arrays.stream(features).boxed().toList()
        );
        // Sendet den POST-Request an den /predict-Endpunkt des ML-Services.
        return restTemplate.postForObject(
                properties.getMlServiceUrl() + "/predict",
                request,
                MlPredictionResponse.class
        );
    }

    /**
     * Sendet eine Batch-Anfrage mit mehreren Feature-Vektoren an den ML-Service,
     * um eine Liste von Vorhersagen zu erhalten. Dies ist optimiert für Backtesting-Szenarien.
     *
     * @param featuresBatch Eine Liste von `double`-Arrays, wobei jedes Array einen Feature-Vektor darstellt.
     * @return Eine Liste von {@link MlPredictionResponse}-Objekten, die die Vorhersagen für jeden Feature-Vektor enthalten.
     *         Gibt eine leere Liste zurück, wenn die Antwort des ML-Services null ist.
     * @throws RestClientException Wenn bei der Kommunikation mit dem ML-Service ein Fehler auftritt.
     */
    public List<MlPredictionResponse> predictBatch(List<double[]> featuresBatch) throws RestClientException {
        // Erstellt ein Request-Objekt für die Batch-Vorhersage.
        Map<String, Object> request = Map.of("batch", featuresBatch);
        // Sendet den POST-Request an den /predict/batch-Endpunkt des ML-Services.
        BatchResponse response = restTemplate.postForObject(
                properties.getMlServiceUrl() + "/predict/batch",
                request,
                BatchResponse.class
        );
        return response != null ? response.predictions() : List.of();
    }

    /**
     * Interner Record zur Deserialisierung der Batch-Vorhersage-Antwort vom ML-Service.
     *
     * @param predictions Eine Liste von {@link MlPredictionResponse}-Objekten.
     */
    private record BatchResponse(List<MlPredictionResponse> predictions) {}

    /**
     * Löst das Training des ML-Modells im externen ML-Service aus.
     * Das Training wird asynchron im ML-Service gestartet.
     *
     * @throws RestClientException Wenn bei der Kommunikation mit dem ML-Service ein Fehler auftritt.
     */
    public void trainModel() throws RestClientException {
        String url = properties.getMlServiceUrl() + "/train";
        log.info("Sende Trainings-Request an: {}", url);
        try {
            // Sendet einen POST-Request an den /train-Endpunkt.
            restTemplate.postForObject(url, Map.of("action", "retrain"), Void.class);
        } catch (Exception e) {
            log.error("Fehler beim Starten des ML-Trainings: {}", e.getMessage());
            throw new RestClientException("ML-Service nicht erreichbar oder Fehler beim Trainingstrigger.");
        }
    }

    /**
     * Ruft den aktuellen Status des Modelltrainings vom ML-Service ab.
     *
     * @return Eine Map, die den Trainingsstatus enthält (z.B. "idle", "running", "success", "error").
     *         Gibt eine Fehlermeldung zurück, wenn der ML-Service nicht erreichbar ist.
     */
    public Map<String, Object> getTrainingStatus() {
        String url = properties.getMlServiceUrl() + "/train/status";
        try {
            // Sendet einen GET-Request an den /train/status-Endpunkt.
            return restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            log.error("Fehler beim Abrufen des Trainingsstatus: {}", e.getMessage());
            return Map.of("status", "error", "error_message", "ML-Service nicht erreichbar.");
        }
    }

    /**
     * Löst den Download und die Vorbereitung neuer Marktdaten im externen ML-Service aus.
     * Der Daten-Download wird asynchron im ML-Service gestartet.
     *
     * @throws RestClientException Wenn bei der Kommunikation mit dem ML-Service ein Fehler auftritt.
     */
    public void triggerDataUpdate() throws RestClientException {
        String url = properties.getMlServiceUrl() + "/data/update";
        log.info("Sende Marktdaten-Update-Request an: {}", url);
        try {
            // Sendet einen POST-Request an den /data/update-Endpunkt.
            restTemplate.postForObject(url, null, Void.class);
        } catch (Exception e) {
            log.error("Fehler beim Starten des Daten-Updates: {}", e.getMessage());
            throw new RestClientException("ML-Service (Downloader) nicht erreichbar oder Fehler beim Daten-Update-Trigger.");
        }
    }

    /**
     * Ruft den aktuellen Status des Marktdaten-Downloads und der Vorbereitung vom ML-Service ab.
     *
     * @return Eine Map, die den Daten-Status enthält (z.B. "idle", "running", "success", "error").
     *         Gibt eine Fehlermeldung zurück, wenn der ML-Service nicht erreichbar ist.
     */
    public Map<String, Object> getDataStatus() {
        String url = properties.getMlServiceUrl() + "/data/status";
        try {
            // Sendet einen GET-Request an den /data/status-Endpunkt.
            return restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            log.error("Fehler beim Abrufen des Datenstatus: {}", e.getMessage());
            return Map.of("status", "error", "error_message", "ML-Service (Downloader) nicht erreichbar.");
        }
    }
}
