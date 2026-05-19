package com.kitradingbot.controller;

import com.kitradingbot.backtesting.BacktestEngine;
import com.kitradingbot.config.TradingProperties;
import com.kitradingbot.model.BacktestResult;
import com.kitradingbot.model.IndicatorConfig;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST-Controller für die Durchführung von Backtests.
 * Ermöglicht es, die Handelsstrategie auf historischen Daten zu simulieren
 * und die Performance zu bewerten.
 */
@RestController
@RequestMapping("/api/backtest")
public class BacktestController {
    private final BacktestEngine backtestEngine;
    private final TradingProperties properties;

    /**
     * Konstruktor für den BacktestController.
     * Spring injiziert automatisch die benötigten Abhängigkeiten.
     *
     * @param backtestEngine Die Engine, die den Backtest ausführt.
     * @param properties     Die Anwendungseigenschaften, die Standardwerte enthalten.
     */
    public BacktestController(BacktestEngine backtestEngine, TradingProperties properties) {
        this.backtestEngine = backtestEngine;
        this.properties = properties;
    }

    /**
     * Startet einen Backtest-Lauf basierend auf den übergebenen Parametern und Indikatorkonfigurationen.
     * Da die Indikatorkonfiguration komplex sein kann, wird ein POST-Request verwendet,
     * um die Liste der {@link IndicatorConfig} im Request Body zu übermitteln.
     *
     * @param symbol    Das zu testende Handelssymbol (z.B. BTCUSDT). Wenn nicht angegeben, wird der Standardwert aus den Properties verwendet.
     * @param candles   Die Anzahl der historischen Kerzen, die analysiert werden sollen. Wenn nicht angegeben, werden 500 Kerzen verwendet.
     * @param useMl     Ein Flag, das angibt, ob die KI-Vorhersage in die Signalgenerierung einbezogen werden soll (Standard: false).
     * @param indicators Eine Liste von {@link IndicatorConfig} Objekten, die die Konfiguration der zu verwendenden Indikatoren definieren.
     *                   Diese werden vom Frontend gesendet.
     * @return Das Ergebnis des Backtests inklusive Performance-Metriken, historischer Kerzen und simulierter Trades.
     */
    @PostMapping
    public BacktestResult runBacktest(
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) Integer candles,
            @RequestParam(defaultValue = "false") boolean useMl,
            @RequestBody(required = false) List<IndicatorConfig> indicators
    ) {
        // Auflösen des Handelssymbols: Wenn nicht vom Client angegeben, den Standard aus den Properties nehmen.
        String resolvedSymbol = symbol == null || symbol.isBlank() ? properties.getSymbol() : symbol;
        // Auflösen der Kerzenanzahl: Wenn nicht vom Client angegeben, den Standardwert 500 nehmen.
        int resolvedCandles = candles == null ? 500 : candles;
        
        // Ausführung des Backtests mit den gesammelten Parametern.
        return backtestEngine.run(resolvedSymbol, resolvedCandles, useMl, indicators);
    }
}
