import { useState, useCallback, useEffect } from "react";
import { createIndicator, sanitizeParam } from "../indicators/tradingViewIndicators";

// Schlüssel für localStorage
const INDICATORS_STORAGE_KEY = "kitradingbot_indicators";
const ML_VOTE_STORAGE_KEY = "kitradingbot_useMlVote";

/**
 * Custom Hook zur Verwaltung der Indikatorkonfiguration im Frontend.
 * Ermöglicht das Hinzufügen, Entfernen und Ändern von Indikatoren
 * sowie die Steuerung des KI-Konsens-Flags.
 * Die Konfiguration wird im localStorage gespeichert, um Persistenz über Seitenneuladungen hinweg zu gewährleisten.
 *
 * @returns {object} Ein Objekt mit dem Indikator-Zustand und den zugehörigen Funktionen.
 */
export function useIndicators() {
  /**
   * @type {[Array<object>, Function]} Der Zustand der konfigurierten Indikatoren.
   * Jeder Indikator ist ein Objekt mit `id`, `type`, `label` und `params`.
   * Initialisiert aus localStorage oder mit Standard-RSI und SMA.
   */
  const [indicators, setIndicators] = useState(() => {
    try {
      const storedIndicators = localStorage.getItem(INDICATORS_STORAGE_KEY);
      return storedIndicators ? JSON.parse(storedIndicators) : [createIndicator("RSI"), createIndicator("SMA")].filter(Boolean);
    } catch (error) {
      console.error("Fehler beim Laden der Indikatoren aus localStorage:", error);
      return [createIndicator("RSI"), createIndicator("SMA")].filter(Boolean);
    }
  });

  /** @type {[boolean, Function]} Flag, das angibt, ob der KI-Konsens aktiviert ist. */
  const [useMlVote, setUseMlVote] = useState(() => {
    try {
      const storedMlVote = localStorage.getItem(ML_VOTE_STORAGE_KEY);
      return storedMlVote !== null ? JSON.parse(storedMlVote) : true; // Standardmäßig aktiviert
    } catch (error) {
      console.error("Fehler beim Laden des useMlVote aus localStorage:", error);
      return true;
    }
  });

  // Effekt zum Speichern der Indikatoren im localStorage bei jeder Änderung
  useEffect(() => {
    try {
      localStorage.setItem(INDICATORS_STORAGE_KEY, JSON.stringify(indicators));
    } catch (error) {
      console.error("Fehler beim Speichern der Indikatoren in localStorage:", error);
    }
  }, [indicators]);

  // Effekt zum Speichern des useMlVote-Flags im localStorage bei jeder Änderung
  useEffect(() => {
    try {
      localStorage.setItem(ML_VOTE_STORAGE_KEY, JSON.stringify(useMlVote));
    } catch (error) {
      console.error("Fehler beim Speichern des useMlVote in localStorage:", error);
    }
  }, [useMlVote]);

  /**
   * Fügt einen neuen Indikator des angegebenen Typs zur Liste hinzu.
   * @param {string} type - Der Typ des hinzuzufügenden Indikators (z.B. "SMA", "RSI").
   */
  const addIndicator = useCallback((type) => {
    const newIndicator = createIndicator(type);
    if (newIndicator) {
      setIndicators((prev) => [...prev, newIndicator]);
    }
  }, []);

  /**
   * Entfernt einen Indikator anhand seiner ID aus der Liste.
   * @param {string} id - Die eindeutige ID des zu entfernenden Indikators.
   */
  const removeIndicator = useCallback((id) => {
    setIndicators((prev) => prev.filter((ind) => ind.id !== id));
  }, []);

  /**
   * Ändert den Wert eines Parameters für einen spezifischen Indikator.
   * Der Wert wird vor der Aktualisierung sanitisiert (z.B. auf Zahlen konvertiert und begrenzt).
   * @param {string} id - Die ID des Indikators, dessen Parameter geändert werden soll.
   * @param {string} key - Der Schlüssel des zu ändernden Parameters (z.B. "length").
   * @param {string|number} value - Der neue Wert des Parameters.
   */
  const changeIndicatorParam = useCallback((id, key, value) => {
    setIndicators((prev) =>
      prev.map((ind) => {
        if (ind.id === id) {
          return {
            ...ind,
            params: {
              ...ind.params,
              [key]: sanitizeParam(ind.type, key, value),
            },
          };
        }
        return ind;
      })
    );
  }, []);

  return {
    indicators,
    useMlVote,
    setUseMlVote,
    addIndicator,
    removeIndicator,
    changeIndicatorParam,
  };
}
