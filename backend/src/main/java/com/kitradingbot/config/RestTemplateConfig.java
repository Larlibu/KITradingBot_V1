package com.kitradingbot.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Konfigurationsklasse für die Bereitstellung eines {@link RestTemplate} Beans.
 * Dieses RestTemplate wird für die Kommunikation mit externen REST-Services,
 * insbesondere dem ML-Service, verwendet.
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Erstellt und konfiguriert ein {@link RestTemplate} Bean.
     * Setzt Timeouts für Verbindungsaufbau und Leseoperationen, um die Robustheit
     * der externen Service-Aufrufe zu verbessern.
     *
     * @param builder Ein {@link RestTemplateBuilder}, bereitgestellt von Spring Boot,
     *                um das RestTemplate zu konfigurieren.
     * @return Eine konfigurierte Instanz von {@link RestTemplate}.
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                // Setzt das Timeout für den Aufbau einer Verbindung zum Ziel-Host.
                .setConnectTimeout(Duration.ofSeconds(5))
                // Setzt das Timeout für das Warten auf Daten nach dem Aufbau der Verbindung.
                // Erhöht auf 10 Sekunden, um dem ML-Service genügend Zeit für komplexere
                // Operationen (z.B. Modell-Training oder Batch-Vorhersagen) zu geben.
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }
}
