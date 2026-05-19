package com.kitradingbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Zentrale Konfigurationsklasse für den Trading-Bot.
 * Liest Werte mit dem Präfix "trading" aus der `application.properties` oder `application.yml` Datei.
 * Diese Eigenschaften steuern das Verhalten des Bots, von Handelsparametern bis hin zu Service-URLs.
 *
 * Die `@ConfigurationProperties`-Annotation macht diese Klasse zu einer Spring Bean,
 * die automatisch mit Werten aus der Konfigurationsdatei befüllt wird.
 * Es ist nicht notwendig, zusätzlich `@Component` zu verwenden, da dies zu doppelten Beans führen würde.
 */
@ConfigurationProperties(prefix = "trading")
public class TradingProperties {
    /** Das zu handelnde Symbol (Standard: BTCUSDT). Wird für Marktdatenabfragen verwendet. */
    private String symbol = "BTCUSDT";
    /** Standardanzahl an Kerzen, die für Marktanfragen (z.B. für das Dashboard) abgerufen werden. */
    private int defaultMarketLimit = 200;
    /** Zeitperiode für den Simple Moving Average (SMA). Wird als Standardwert verwendet, wenn keine dynamischen Indikatoren übergeben werden. */
    private int smaPeriod = 14;
    /** Zeitperiode für den Relative Strength Index (RSI). Wird als Standardwert verwendet, wenn keine dynamischen Indikatoren übergeben werden. */
    private int rsiPeriod = 14;
    /** Minimaler Konfidenzschwellenwert (0.0 bis 1.0) der ML-Vorhersage, damit diese als gültiges Signal zählt. */
    private double mlConfidenceThreshold = 0.60;
    /** RSI-Wert, ab dem der Markt als überkauft gilt (z.B. 70.0). */
    private double rsiOverbought = 70.0;
    /** RSI-Wert, ab dem der Markt als überverkauft gilt (z.B. 30.0). */
    private double rsiOversold = 30.0;
    /** Anteil des Kapitals, der pro Trade riskiert wird (z.B. 0.01 = 1%). Beeinflusst die Positionsgröße. */
    private double riskPerTrade = 0.01;
    /** Startkapital für die Backtest-Simulationen. */
    private double initialCapital = 10_000.0;
    /** URL zum externen Machine Learning Microservice (z.B. http://localhost:8000). */
    private String mlServiceUrl = "http://localhost:8000";

    // --- Getter und Setter ---
    // (Standardmäßig von Spring Boot für @ConfigurationProperties generiert oder manuell hinzugefügt)

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public int getDefaultMarketLimit() {
        return defaultMarketLimit;
    }

    public void setDefaultMarketLimit(int defaultMarketLimit) {
        this.defaultMarketLimit = defaultMarketLimit;
    }

    public int getSmaPeriod() {
        return smaPeriod;
    }

    public void setSmaPeriod(int smaPeriod) {
        this.smaPeriod = smaPeriod;
    }

    public int getRsiPeriod() {
        return rsiPeriod;
    }

    public void setRsiPeriod(int rsiPeriod) {
        this.rsiPeriod = rsiPeriod;
    }

    public double getMlConfidenceThreshold() {
        return mlConfidenceThreshold;
    }

    public void setMlConfidenceThreshold(double mlConfidenceThreshold) {
        this.mlConfidenceThreshold = mlConfidenceThreshold;
    }

    public double getRsiOverbought() {
        return rsiOverbought;
    }

    public void setRsiOverbought(double rsiOverbought) {
        this.rsiOverbought = rsiOverbought;
    }

    public double getRsiOversold() {
        return rsiOversold;
    }

    public void setRsiOversold(double rsiOversold) {
        this.rsiOversold = rsiOversold;
    }

    public double getRiskPerTrade() {
        return riskPerTrade;
    }

    public void setRiskPerTrade(double riskPerTrade) {
        this.riskPerTrade = riskPerTrade;
    }

    public double getInitialCapital() {
        return initialCapital;
    }

    public void setInitialCapital(double initialCapital) {
        this.initialCapital = initialCapital;
    }

    public String getMlServiceUrl() {
        return mlServiceUrl;
    }

    public void setMlServiceUrl(String mlServiceUrl) {
        this.mlServiceUrl = mlServiceUrl;
    }
}
