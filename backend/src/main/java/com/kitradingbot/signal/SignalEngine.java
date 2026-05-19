package com.kitradingbot.signal;

import com.kitradingbot.model.Candle;
import com.kitradingbot.model.IndicatorConfig;
import com.kitradingbot.model.MlPredictionResponse;
import com.kitradingbot.model.SignalDecision;

import java.util.List;

/**
 * Schnittstelle für die Generierung von Handelssignalen.
 * Eine SignalEngine ist dafür verantwortlich, aus einer Kombination von
 * Marktdaten, technischen Indikatoren und optionalen KI-Vorhersagen
 * eine konkrete Handelsentscheidung (Kaufen, Verkaufen, Halten) abzuleiten.
 */
public interface SignalEngine {
    /**
     * Generiert eine Signalentscheidung basierend auf den aktuellen Marktdaten,
     * einer optionalen KI-Vorhersage und einer Liste dynamisch konfigurierter Indikatoren.
     *
     * @param candles    Eine {@link List} von {@link Candle}-Objekten, die die historischen Marktdaten darstellen.
     *                   Die letzte Kerze in dieser Liste ist die aktuelle Kerze, für die das Signal generiert wird.
     * @param prediction Das {@link MlPredictionResponse}-Objekt, das die Vorhersage des Machine-Learning-Modells enthält.
     *                   Kann `null` sein, wenn keine KI-Vorhersage verfügbar oder gewünscht ist.
     * @param useMl      Ein `boolean`-Flag, das angibt, ob die KI-Vorhersage in die Konsensbildung für das Signal einbezogen werden soll.
     * @param indicators Eine {@link List} von {@link IndicatorConfig}-Objekten, die die Konfiguration der
     *                   vom Benutzer ausgewählten Indikatoren definieren.
     * @return Eine {@link SignalDecision}, die das generierte Signal (BUY, SELL, HOLD),
     *         eine Begründung und die Konfidenz der Entscheidung enthält.
     */
    SignalDecision generateSignal(List<Candle> candles, MlPredictionResponse prediction, boolean useMl, List<IndicatorConfig> indicators);
}
