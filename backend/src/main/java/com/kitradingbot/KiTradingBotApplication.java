package com.kitradingbot;

import com.kitradingbot.config.TradingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Die Hauptklasse der KI Trading Bot Anwendung.
 * Diese Klasse startet den Spring Boot Kontext und aktiviert die Konfigurationseigenschaften.
 */
@SpringBootApplication
@EnableConfigurationProperties(TradingProperties.class)
public class KiTradingBotApplication {

    /**
     * Haupteinstiegspunkt für die Anwendung.
     * @param args Kommandozeilenargumente.
     */
    public static void main(String[] args) {
        SpringApplication.run(KiTradingBotApplication.class, args);
    }
}
