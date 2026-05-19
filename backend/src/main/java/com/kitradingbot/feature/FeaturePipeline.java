package com.kitradingbot.feature;

import com.kitradingbot.model.Candle;
import com.kitradingbot.model.IndicatorConfig;
import java.util.List;

/**
 * Schnittstelle zur Umwandlung von Rohdaten (Kerzen) in Feature-Vektoren für die KI.
 * Eine Feature-Pipeline ist dafür verantwortlich, aus einer Reihe von Marktdaten (Candles)
 * einen numerischen Vektor zu erzeugen, der als Eingabe für ein Machine-Learning-Modell dienen kann.
 */
public interface FeaturePipeline {
    /**
     * Erstellt den Feature-Vektor für die aktuellste Kerze in der gegebenen Liste.
     * Die Berechnung berücksichtigt die vom Benutzer konfigurierten Indikatoren.
     *
     * @param candles    Eine {@link List} von {@link Candle}-Objekten, die die historischen Marktdaten darstellen.
     * @param indicators Eine {@link List} von {@link IndicatorConfig}-Objekten, die die Konfiguration der zu verwendenden Indikatoren definieren.
     * @return Ein `double`-Array, das den Feature-Vektor für die KI darstellt.
     */
    double[] buildFeatures(List<Candle> candles, List<IndicatorConfig> indicators);

    /**
     * Gibt die Namen der Features in der Reihenfolge zurück, in der sie im Feature-Vektor erscheinen.
     * Dies ist nützlich für Debugging und zur Verständlichkeit der ML-Eingabe.
     *
     * @return Eine {@link List} von Strings, die die Namen der Features enthält.
     */
    List<String> featureNames();

    /**
     * Berechnet die minimale Anzahl an Kerzen, die benötigt werden, um alle Features
     * (d.h. alle Indikatoren, die in der Pipeline verwendet werden) berechnen zu können.
     * Dies ist die "Warmup-Periode" des Systems.
     *
     * @return Die minimale Anzahl von Kerzen (als `int`).
     */
    int warmupPeriod();
    
    /**
     * Hilfsmethode zum Erstellen von Features ohne dynamische Indikatorkonfiguration.
     * Diese Methode dient der Abwärtskompatibilität oder für Fälle, in denen keine
     * spezifischen Indikatoren vom Frontend übergeben werden.
     *
     * @param candles Eine {@link List} von {@link Candle}-Objekten.
     * @return Ein `double`-Array, das den Feature-Vektor darstellt.
     */
    default double[] buildFeatures(List<Candle> candles) {
        return buildFeatures(candles, List.of());
    }
}
