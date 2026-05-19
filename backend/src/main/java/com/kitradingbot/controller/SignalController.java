package com.kitradingbot.controller;

import com.kitradingbot.config.TradingProperties;
import com.kitradingbot.model.IndicatorConfig;
import com.kitradingbot.model.SignalSnapshot;
import com.kitradingbot.service.SignalService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST-Controller für den Abruf aktueller Handelssignale.
 * Dieser Controller dient als Schnittstelle für das Frontend, um den aktuellen
 * Marktzustand und die daraus resultierende Handelsentscheidung zu erhalten.
 */
@RestController
@RequestMapping("/api/signal")
public class SignalController {
    private final SignalService signalService;
    private final TradingProperties properties;

    /**
     * Konstruktor für den SignalController.
     * Spring injiziert automatisch die benötigten Abhängigkeiten.
     *
     * @param signalService Der Service, der für die Signalgenerierung zuständig ist.
     * @param properties    Die Anwendungseigenschaften, die Standardwerte enthalten.
     */
    public SignalController(SignalService signalService, TradingProperties properties) {
        this.signalService = signalService;
        this.properties = properties;
    }

    /**
     * Ermittelt das aktuellste Signal für ein Symbol basierend auf den neuesten Marktdaten
     * und den vom Frontend übermittelten Indikatorkonfigurationen.
     * Da die Indikatorkonfiguration komplex sein kann, wird ein POST-Request verwendet,
     * um die Liste der {@link IndicatorConfig} im Request Body zu übermitteln.
     *
     * @param symbol     Das Handelssymbol (z.B. BTCUSDT). Optional; wenn nicht angegeben, wird der Standardwert aus den Properties verwendet.
     * @param candles    Die Anzahl der Kerzen, die für die Analyse herangezogen werden sollen. Optional; wenn nicht angegeben, wird der Standardwert aus den Properties verwendet.
     * @param useMl      Ein Flag, das angibt, ob die KI-Vorhersage in die Signalgenerierung einbezogen werden soll (Standard: false).
     * @param indicators Eine Liste von {@link IndicatorConfig} Objekten, die die Konfiguration der zu verwendenden Indikatoren definieren.
     *                   Diese werden vom Frontend gesendet.
     * @return Ein {@link SignalSnapshot} mit allen Analyseergebnissen, der KI-Vorhersage und der finalen Handelsentscheidung.
     */
    @PostMapping
    public SignalSnapshot currentSignal(
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) Integer candles,
            @RequestParam(defaultValue = "false") boolean useMl,
            @RequestBody(required = false) List<IndicatorConfig> indicators
    ) {
        // Auflösen des Handelssymbols: Wenn nicht vom Client angegeben, den Standard aus den Properties nehmen.
        String resolvedSymbol = symbol == null || symbol.isBlank() ? properties.getSymbol() : symbol;
        // Auflösen der Kerzenanzahl: Wenn nicht vom Client angegeben, den Standard aus den Properties nehmen.
        int resolvedCandles = candles == null ? properties.getDefaultMarketLimit() : candles;
        
        // Abrufen des aktuellen Signals über den Service mit allen Parametern.
        return signalService.currentSignal(resolvedSymbol, resolvedCandles, useMl, indicators);
    }
}
