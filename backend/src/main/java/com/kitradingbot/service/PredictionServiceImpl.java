package com.kitradingbot.service;

import com.kitradingbot.integration.MlServiceClient;
import com.kitradingbot.model.MlPredictionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

/**
 * Implementierung des {@link PredictionService}.
 * Diese Service-Klasse kommuniziert mit einem externen Machine Learning Microservice (z.B. Python/FastAPI)
 * über REST, um Vorhersagen zu erhalten oder das Modell zu trainieren.
 * Sie beinhaltet eine Fallback-Logik, falls der externe Dienst nicht erreichbar ist.
 */
@Service
public class PredictionServiceImpl implements PredictionService {
    private static final Logger log = LoggerFactory.getLogger(PredictionServiceImpl.class);
    private final MlServiceClient mlServiceClient;

    /**
     * Konstruktor für den PredictionServiceImpl.
     * Spring injiziert automatisch den benötigten {@link MlServiceClient}.
     *
     * @param mlServiceClient Der Client für die Kommunikation mit dem ML-Service.
     */
    public PredictionServiceImpl(MlServiceClient mlServiceClient) {
        this.mlServiceClient = mlServiceClient;
    }

    /**
     * Sendet einen Feature-Vektor an den ML-Service und gibt die Vorhersage zurück.
     * Bei einem Fehler (z.B. ML-Dienst nicht erreichbar) wird eine einfache Fallback-Vorhersage
     * basierend auf dem letzten Feature-Wert (z.B. close_minus_sma) erstellt.
     *
     * @param features Ein `double`-Array, das den Feature-Vektor für die Vorhersage darstellt.
     * @return Ein {@link MlPredictionResponse}-Objekt mit dem vorhergesagten Label und der Konfidenz.
     * @throws TradingBotException wenn der ML-Dienst eine leere oder ungültige Antwort liefert.
     */
    @Override
    public MlPredictionResponse predict(double[] features) {
        try {
            MlPredictionResponse response = mlServiceClient.predict(features);
            if (response == null || response.label() == null) {
                throw new TradingBotException("ML-Dienst lieferte eine leere oder ungültige Antwort.");
            }
            return response;
        } catch (RestClientException ex) {
            log.warn("ML-Dienst nicht erreichbar oder Fehler bei der Vorhersage, nutze Fallback-Logik: {}", ex.getMessage());
            
            // Simples Fallback: Wenn das letzte Feature (z.B. close_minus_sma) positiv ist,
            // wird "STEIGT" angenommen, ansonsten "FÄLLT". Konfidenz ist 50%.
            String fallbackLabel = features[features.length - 1] >= 0 ? "STEIGT" : "FÄLLT";
            return new MlPredictionResponse(fallbackLabel, 0.50);
        }
    }

    /**
     * Leitet den Trainings-Request an den {@link MlServiceClient} weiter.
     *
     * @throws TradingBotException wenn der Trainingsvorgang nicht ausgelöst werden kann.
     */
    @Override
    public void triggerMlTraining() {
        log.info("Triggering ML model training via integration client.");
        try {
            mlServiceClient.trainModel();
        } catch (RestClientException ex) {
            log.error("Failed to trigger ML training: {}", ex.getMessage());
            throw new TradingBotException("Der externe ML-Service konnte das Training nicht starten: " + ex.getMessage());
        }
    }
}
