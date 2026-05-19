package com.kitradingbot.service;

import com.kitradingbot.model.IndicatorConfig;
import com.kitradingbot.model.SignalSnapshot;
import java.util.List;

/**
 * Interface für den Service, der den aktuellen Marktstatus analysiert und Handelssignale generiert.
 * Dieser Service ist die zentrale Anlaufstelle für das Frontend, um den aktuellen
 * Zustand der Strategie und die daraus resultierende Entscheidung abzufragen.
 */
public interface SignalService {
    /**
     * Erstellt einen aktuellen Zustandsbericht (Snapshot) inklusive der finalen Signalentscheidung.
     * Diese Methode führt die gesamte Analyse-Pipeline aus: Marktdaten abrufen, Features berechnen,
     * KI-Vorhersage einholen und die Handelsentscheidung basierend auf der Strategie generieren.
     *
     * @param symbol     Das Handelssymbol, für das die Analyse durchgeführt werden soll (z.B. "BTCUSDT").
     * @param candles    Die Anzahl der historischen Kerzen, die für die Analyse herangezogen werden sollen.
     * @param useMl      Ein Flag, das angibt, ob die KI-Vorhersage in die Signalgenerierung einbezogen werden soll.
     * @param indicators Eine {@link List} von {@link IndicatorConfig}-Objekten, die die Konfiguration der
     *                   vom Benutzer ausgewählten Indikatoren definieren.
     * @return Ein {@link SignalSnapshot}-Objekt, das alle relevanten Informationen über den aktuellen
     *         Marktzustand und die Handelsentscheidung enthält.
     */
    SignalSnapshot currentSignal(String symbol, int candles, boolean useMl, List<IndicatorConfig> indicators);
}
