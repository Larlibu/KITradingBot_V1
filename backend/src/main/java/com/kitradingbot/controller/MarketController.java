package com.kitradingbot.controller;

import com.kitradingbot.config.TradingProperties;
import com.kitradingbot.model.Candle;
import com.kitradingbot.service.MarketDataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST-Controller für den Zugriff auf Marktdaten.
 * Ermöglicht das Abrufen von historischen Preisdaten (Candlesticks) für die Anzeige im Frontend
 * oder für andere Analyse-Zwecke.
 */
@RestController
@RequestMapping("/api/market")
public class MarketController {
    private final MarketDataService marketDataService;
    private final TradingProperties properties;

    /**
     * Konstruktor für den MarketController.
     * Spring injiziert automatisch die benötigten Abhängigkeiten.
     *
     * @param marketDataService Der Service, der für das Laden der Marktdaten zuständig ist.
     * @param properties        Die globalen Trading-Konfigurationen, die Standardwerte wie Symbol und Limit enthalten.
     */
    public MarketController(MarketDataService marketDataService, TradingProperties properties) {
        this.marketDataService = marketDataService;
        this.properties = properties;
    }

    /**
     * Ruft historische Marktdaten (Candlesticks) für ein bestimmtes Handelssymbol ab.
     * Die Anzahl der zurückgegebenen Kerzen kann über den 'limit'-Parameter gesteuert werden.
     *
     * @param symbol Das Handelspaar (z.B. "BTCUSDT"). Optional; wenn nicht angegeben, wird das Standard-Symbol aus den {@link TradingProperties} verwendet.
     * @param limit  Die maximale Anzahl der zurückzugebenden Kerzen. Optional; wenn nicht angegeben, wird das Standard-Limit aus den {@link TradingProperties} verwendet.
     * @return Eine {@link List} von {@link Candle}-Objekten, die die historischen Marktdaten repräsentieren.
     */
    @GetMapping
    public List<Candle> getMarketData(
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) Integer limit
    ) {
        // Auflösen des Handelssymbols: Wenn nicht vom Client angegeben, den Standard aus den Properties nehmen.
        String resolvedSymbol = symbol == null || symbol.isBlank() ? properties.getSymbol() : symbol;
        // Auflösen des Limits: Wenn nicht vom Client angegeben, den Standard aus den Properties nehmen.
        int resolvedLimit = limit == null ? properties.getDefaultMarketLimit() : limit;
        
        // Abrufen der Marktdaten über den Service.
        return marketDataService.getMarketData(resolvedSymbol, resolvedLimit);
    }
}
