package com.kitradingbot.feature;

import com.kitradingbot.indicator.RsiIndicator;
import com.kitradingbot.indicator.SmaIndicator;
import com.kitradingbot.model.Candle;
import com.kitradingbot.model.IndicatorConfig;
import com.kitradingbot.service.TradingBotException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Implementierung der {@link FeaturePipeline} basierend auf technischen Indikatoren.
 * Diese Pipeline ist dafür verantwortlich, aus einer Liste von {@link Candle}-Objekten
 * einen numerischen Feature-Vektor zu erstellen, der als Eingabe für das Machine-Learning-Modell dient.
 * Die Indikatorparameter (z.B. Periodenlängen) können dynamisch vom Frontend übergeben werden.
 */
@Component
public class IndicatorFeaturePipeline implements FeaturePipeline {
    /**
     * Namen der in dieser Pipeline enthaltenen Features in der Reihenfolge,
     * in der sie im Feature-Vektor erscheinen. Diese Reihenfolge muss mit der
     * Erwartung des ML-Modells übereinstimmen.
     */
    private static final List<String> FEATURE_NAMES = List.of("close", "sma", "rsi", "close_minus_sma");
    
    private final SmaIndicator smaIndicator;
    private final RsiIndicator rsiIndicator;

    /**
     * Konstruktor für die IndicatorFeaturePipeline.
     * Spring injiziert automatisch die benötigten Indikator-Implementierungen.
     *
     * @param smaIndicator Die Implementierung des {@link SmaIndicator}.
     * @param rsiIndicator Die Implementierung des {@link RsiIndicator}.
     */
    public IndicatorFeaturePipeline(SmaIndicator smaIndicator, RsiIndicator rsiIndicator) {
        this.smaIndicator = smaIndicator;
        this.rsiIndicator = rsiIndicator;
    }

    /**
     * Erstellt den Feature-Vektor für die aktuellste Kerze in der Liste.
     * Der Vektor enthält den Schlusskurs, SMA, RSI und die Differenz zwischen Schlusskurs und SMA.
     * Die Perioden für SMA und RSI werden dynamisch aus der übergebenen Indikatorkonfiguration extrahiert.
     *
     * @param candles    Eine Liste von {@link Candle}-Objekten, die die historischen Marktdaten darstellen.
     *                   Die letzte Kerze in dieser Liste ist die aktuelle Kerze, für die Features berechnet werden.
     * @param indicators Eine Liste von {@link IndicatorConfig}-Objekten, die die Konfiguration der
     *                   zu verwendenden Indikatoren definieren.
     * @return Ein `double`-Array mit den Werten [Schlusskurs, SMA-Wert, RSI-Wert, Differenz Schluss-SMA].
     * @throws TradingBotException wenn nicht genügend Marktdaten vorhanden sind.
     */
    @Override
    public double[] buildFeatures(List<Candle> candles, List<IndicatorConfig> indicators) {
        if (candles == null || candles.isEmpty()) {
            throw new TradingBotException("Keine Marktdaten vorhanden, um den Feature-Vektor zu erstellen.");
        }
        
        int idx = candles.size() - 1;
        double close = candles.get(idx).close();
        
        // Standardwerte für Indikatorperioden, falls keine dynamischen Konfigurationen übergeben werden
        int smaLen = 20;
        int rsiLen = 14;

        // Extrahiere dynamische Perioden aus der übergebenen Indikatorkonfiguration
        if (indicators != null) {
            for (IndicatorConfig config : indicators) {
                if ("SMA".equalsIgnoreCase(config.type())) {
                    smaLen = config.getIntParam("length", 20);
                } else if ("RSI".equalsIgnoreCase(config.type())) {
                    rsiLen = config.getIntParam("length", 14);
                }
                // Hier könnten weitere Indikatoren für die Feature-Berechnung hinzugefügt werden,
                // falls das ML-Modell diese als Features benötigt.
            }
        }

        // Berechne die Indikatoren mit den (dynamischen oder Standard-)Perioden
        double sma = smaIndicator.calculate(candles, idx, smaLen);
        double rsi = rsiIndicator.calculate(candles, idx, rsiLen);
        
        // Erstelle den Feature-Vektor. Die Reihenfolge muss 1:1 mit der Erwartung des ML-Modells übereinstimmen.
        return new double[]{close, sma, rsi, close - sma};
    }

    /**
     * Gibt die Liste der Namen der Features zurück, die von dieser Pipeline generiert werden.
     *
     * @return Eine {@link List} von Strings, die die Namen der Features enthält.
     */
    @Override
    public List<String> featureNames() {
        return FEATURE_NAMES;
    }

    /**
     * Berechnet die minimale Anzahl an Kerzen, die benötigt werden, um alle Indikatoren
     * (SMA und RSI in diesem Fall) für die Feature-Berechnung zu initialisieren.
     * Ein konservativer Wert von 30 wird verwendet, um sicherzustellen, dass genügend Daten
     * für die meisten gängigen Perioden vorhanden sind.
     *
     * @return Die minimale Anzahl von Kerzen (als `int`) für den Warmup.
     */
    @Override
    public int warmupPeriod() {
        // Ein konservativer Warmup-Wert, der die meisten gängigen SMA/RSI-Perioden abdeckt.
        // Dies stellt sicher, dass die Indikatoren stabile Werte liefern können.
        return 30;
    }
}
