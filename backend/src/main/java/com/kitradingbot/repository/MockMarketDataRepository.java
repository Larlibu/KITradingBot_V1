package com.kitradingbot.repository;

import com.kitradingbot.config.TradingProperties;
import com.kitradingbot.model.Candle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class MockMarketDataRepository implements MarketDataRepository {
    private static final Logger log = LoggerFactory.getLogger(MockMarketDataRepository.class);
    private final RestTemplate restTemplate;
    private final TradingProperties properties;

    @Value("${trading.binance-url}")
    private String binanceUrl;

    public MockMarketDataRepository(RestTemplate restTemplate, TradingProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @Override
    public List<Candle> fetchRecent(String symbol, int limit) {
        if (limit > 300) {
            List<Candle> historical = fetchFromMlService(limit);
            // Fallback auf Binance, falls ML-Service keine Daten hat (z.B. CSV leer)
            if (historical.isEmpty()) {
                log.warn("Keine historischen Daten im ML-Service gefunden. Nutze Binance als Fallback.");
                return fetchFromBinance(symbol, limit);
            }
            return historical;
        } else {
            return fetchFromBinance(symbol, limit);
        }
    }

    private List<Candle> fetchFromBinance(String symbol, int limit) {
        String url = String.format("%s?symbol=%s&interval=1h&limit=%d", binanceUrl, symbol, limit);
        try {
            List<List<Object>> response = restTemplate.getForObject(url, List.class);
            List<Candle> candles = new ArrayList<>();
            if (response == null) return candles;
            for (List<Object> entry : response) {
                candles.add(new Candle(
                        LocalDateTime.ofInstant(Instant.ofEpochMilli(((Number) entry.get(0)).longValue()), ZoneId.systemDefault()),
                        Double.parseDouble((String) entry.get(1)),
                        Double.parseDouble((String) entry.get(2)),
                        Double.parseDouble((String) entry.get(3)),
                        Double.parseDouble((String) entry.get(4)),
                        Double.parseDouble((String) entry.get(5))
                ));
            }
            return candles;
        } catch (Exception e) {
            log.error("Binance API Fehler: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<Candle> fetchFromMlService(int limit) {
        String url = properties.getMlServiceUrl() + "/data/candles?limit=" + limit;
        try {
            List<Map<String, Object>> rawCandles = restTemplate.getForObject(url, List.class);
            List<Candle> candles = new ArrayList<>();
            if (rawCandles == null) return candles;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (Map<String, Object> raw : rawCandles) {
                candles.add(new Candle(
                        LocalDateTime.parse((String) raw.get("timestamp"), formatter),
                        ((Number) raw.get("open")).doubleValue(),
                        ((Number) raw.get("high")).doubleValue(),
                        ((Number) raw.get("low")).doubleValue(),
                        ((Number) raw.get("close")).doubleValue(),
                        ((Number) raw.get("volume")).doubleValue()
                ));
            }
            return candles;
        } catch (Exception e) {
            log.error("ML-Service Fehler bei Kerzen-Abruf: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
