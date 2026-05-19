package com.kitradingbot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web-Konfigurationsklasse für die Spring Boot Anwendung.
 * Konfiguriert unter anderem CORS (Cross-Origin Resource Sharing),
 * um Anfragen vom Frontend (React) zu erlauben.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Konfiguriert CORS-Mappings für die Anwendung.
     * Erlaubt dem Frontend, auf die REST-API zuzugreifen, auch wenn es von einer
     * anderen Domain/Port kommt (was bei der Entwicklung üblich ist).
     *
     * @param registry Der {@link CorsRegistry} zur Konfiguration der CORS-Regeln.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Erlaube CORS für alle Endpunkte ("/**")
        registry.addMapping("/**")
                // Erlaubt Anfragen von den Entwicklungsserver-Adressen des Frontends.
                // Dies ist wichtig, da das Frontend (Vite) standardmäßig auf Port 5173 läuft
                // und das Backend auf Port 8080.
                .allowedOrigins("http://localhost:5173", "http://127.0.0.1:5173")
                // Erlaubt die gängigsten HTTP-Methoden. OPTIONS ist für Preflight-Requests wichtig.
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // Erlaubt alle Header in den Anfragen.
                .allowedHeaders("*");
    }
}
