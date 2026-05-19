package com.kitradingbot.model;

/**
 * Aufzählung der möglichen Signal- oder Handelstypen, die von der Strategie generiert werden können.
 * Diese Typen definieren die Art der empfohlenen Handelsaktion.
 */
public enum SignalType {
    /**
     * Repräsentiert ein Kaufsignal oder die Empfehlung, eine Long-Position zu eröffnen.
     */
    BUY,
    /**
     * Repräsentiert ein Verkaufssignal oder die Empfehlung, eine bestehende Position zu schließen.
     */
    SELL,
    /**
     * Repräsentiert den Zustand, in dem kein klares Signal vorliegt und kein Handlungsbedarf besteht.
     */
    HOLD
}
