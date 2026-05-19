package com.kitradingbot.trading;

import com.kitradingbot.config.TradingProperties;
import com.kitradingbot.model.Candle;
import com.kitradingbot.model.SignalDecision;
import com.kitradingbot.model.SignalType;
import com.kitradingbot.model.Trade;
import com.kitradingbot.repository.TradeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Eine simulierte Implementierung der {@link TradingEngine}.
 * Diese Engine führt keine echten Trades an einer Börse aus, sondern simuliert
 * den Handel basierend auf den generierten Signalen und verwaltet ein virtuelles Kapital.
 * Sie speichert die simulierten Trades in einem {@link TradeRepository}.
 */
@Component
public class SimulatedTradingEngine implements TradingEngine {
    private static final Logger log = LoggerFactory.getLogger(SimulatedTradingEngine.class);

    private final TradeRepository tradeRepository;
    private final TradingProperties properties;

    private boolean running; // Gibt an, ob der Bot aktiv Trades ausführen soll
    private double capital; // Aktuelles virtuelles Kapital
    private double openQuantity; // Menge des aktuell gehaltenen Assets
    private double entryPrice; // Einstiegspreis der aktuellen offenen Position

    /**
     * Konstruktor für die SimulatedTradingEngine.
     * Spring injiziert automatisch die benötigten Abhängigkeiten.
     *
     * @param tradeRepository Das Repository zum Speichern und Abrufen von simulierten Trades.
     * @param properties      Die globalen Trading-Konfigurationen, die das Startkapital und das Risiko pro Trade enthalten.
     */
    public SimulatedTradingEngine(TradeRepository tradeRepository, TradingProperties properties) {
        this.tradeRepository = tradeRepository;
        this.properties = properties;
        this.capital = properties.getInitialCapital(); // Initialisierung mit dem Startkapital
    }

    /**
     * Verarbeitet eine Handelsentscheidung und führt einen simulierten Trade aus, falls der Bot aktiv ist.
     * Diese Methode ist synchronisiert, um Race Conditions bei der Aktualisierung des Handelszustands zu vermeiden.
     *
     * @param decision Die {@link SignalDecision}, die das zu verarbeitende Signal enthält.
     * @param candle   Die aktuelle {@link Candle}, auf deren Basis die Entscheidung getroffen wurde.
     */
    @Override
    public synchronized void execute(SignalDecision decision, Candle candle) {
        // Wenn der Bot nicht läuft, keine Trades ausführen.
        if (!running) {
            return;
        }

        // Kauf-Logik: Wenn ein BUY-Signal vorliegt und keine Position offen ist.
        if (decision.signal() == SignalType.BUY && openQuantity == 0.0) {
            // Berechnet die Positionsgröße basierend auf dem definierten Risiko pro Trade.
            double riskBudget = capital * properties.getRiskPerTrade();
            double quantity = riskBudget / candle.close();

            if (quantity <= 0) {
                log.warn("Nicht genügend Kapital für einen Kauf bei Preis {} mit Budget {}. Kapital: {}", candle.close(), riskBudget, capital);
                return; // Nicht genügend Kapital für eine sinnvolle Position
            }

            openQuantity = quantity;
            entryPrice = candle.close();
            
            // Speichert den simulierten Kauf-Trade.
            Trade entry = new Trade(
                    tradeRepository.nextId(),
                    candle.timestamp(),
                    SignalType.BUY,
                    candle.close(),
                    quantity,
                    0.0, // PnL ist beim Kauf 0
                    capital // Kapital ändert sich erst beim Verkauf
            );
            tradeRepository.save(entry);
            log.info("Position eröffnet: Menge={} Preis={}", quantity, candle.close());
        } 
        // Verkauf-Logik: Wenn ein SELL-Signal vorliegt und eine Position offen ist.
        else if (decision.signal() == SignalType.SELL && openQuantity > 0.0) {
            // Berechnet den Gewinn oder Verlust der geschlossenen Position.
            double pnl = (candle.close() - entryPrice) * openQuantity;
            capital += pnl; // Aktualisiert das virtuelle Kapital
            
            // Speichert den simulierten Verkauf-Trade.
            Trade exit = new Trade(
                    tradeRepository.nextId(),
                    candle.timestamp(),
                    SignalType.SELL,
                    candle.close(),
                    openQuantity,
                    pnl,
                    capital
            );
            tradeRepository.save(exit);
            log.info("Position geschlossen: Menge={} Preis={} PnL={}", openQuantity, candle.close(), pnl);
            
            // Setzt die offenen Positionsdaten zurück.
            openQuantity = 0.0;
            entryPrice = 0.0;
        }
    }

    /**
     * Ruft eine Liste aller seit dem letzten Reset durchgeführten simulierten Trades ab.
     * Diese Methode ist synchronisiert, um konsistente Daten zu gewährleisten.
     *
     * @return Eine {@link List} von {@link Trade}-Objekten.
     */
    @Override
    public synchronized List<Trade> getTrades() {
        return tradeRepository.findAll();
    }

    /**
     * Setzt den Zustand der Trading-Engine zurück.
     * Dies löscht alle gespeicherten Trades, setzt die offene Position zurück
     * und stellt das Kapital auf den initialen Wert wieder her.
     * Diese Methode ist synchronisiert.
     */
    @Override
    public synchronized void reset() {
        tradeRepository.clear(); // Löscht alle Trades
        openQuantity = 0.0;
        entryPrice = 0.0;
        capital = properties.getInitialCapital(); // Setzt Kapital zurück
        log.info("Trading-Engine zurückgesetzt. Kapital: {}", capital);
    }

    /**
     * Startet den automatisierten Handel.
     * Nach dem Aufruf dieser Methode wird die Engine Signale verarbeiten und Trades ausführen.
     * Diese Methode ist synchronisiert.
     */
    @Override
    public synchronized void start() {
        running = true;
        log.info("Trading-Engine gestartet.");
    }

    /**
     * Stoppt den automatisierten Handel.
     * Die Engine wird keine weiteren Trades ausführen, bis sie erneut gestartet wird.
     * Offene Positionen bleiben bestehen, werden aber nicht automatisch geschlossen.
     * Diese Methode ist synchronisiert.
     */
    @Override
    public synchronized void stop() {
        running = false;
        log.info("Trading-Engine gestoppt.");
    }

    /**
     * Überprüft, ob die Trading-Engine aktuell aktiv ist und Trades ausführt.
     * Diese Methode ist synchronisiert.
     *
     * @return `true`, wenn der Bot läuft; `false` sonst.
     */
    @Override
    public synchronized boolean isRunning() {
        return running;
    }
}
