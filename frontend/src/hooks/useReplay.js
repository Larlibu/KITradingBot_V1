import { useState, useCallback, useEffect, useRef } from "react";

/**
 * Custom Hook zur Verwaltung der Replay-Funktionalität im Analyse-Modus.
 * Ermöglicht das schrittweise Durchlaufen von Marktdaten, automatisches Abspielen,
 * das Stoppen bei Handelssignalen und die Anzeige von Replay-Markern.
 *
 * @param {Array<object>} marketData - Die vollständigen historischen Marktdaten (Candles).
 * @param {Function} evaluateDecisionAtIndex - Callback-Funktion zur Evaluierung der Strategie für eine Kerze.
 * @returns {object} Ein Objekt mit Replay-Zustand und Steuerungsfunktionen.
 */
export function useReplay(marketData, evaluateDecisionAtIndex) {
  /** @type {[boolean, Function]} Gibt an, ob der Replay-Modus aktiviert ist. */
  const [replayEnabled, setReplayEnabled] = useState(false);
  /** @type {[number, Function]} Der aktuelle Index der im Replay angezeigten Kerze. */
  const [replayIndex, setReplayIndex] = useState(0);
  /** @type {[object|null, Function]} Das Ergebnis der Strategie-Evaluierung für die aktuelle Replay-Kerze. */
  const [replaySignal, setReplaySignal] = useState(null);
  /** @type {[Array<object>, Function]} Marker, die im Chart angezeigt werden sollen (z.B. für Trades). */
  const [replayMarkers, setReplayMarkers] = useState([]);
  /** @type {[boolean, Function]} Ladezustand, während eine Replay-Kerze evaluiert wird. */
  const [replayLoading, setReplayLoading] = useState(false);

  /** @type {[boolean, Function]} Gibt an, ob das automatische Abspielen aktiv ist. */
  const [isPlaying, setIsPlaying] = useState(false);
  /** @type {[number, Function]} Die Wiedergabegeschwindigkeit in Millisekunden pro Kerze. */
  const [playbackSpeed, setPlaybackSpeed] = useState(500);
  /** @type {[object|null, Function]} Informationen über das letzte Handelssignal, das einen Stopp ausgelöst hat. */
  const [lastTradeSignal, setLastTradeSignal] = useState(null);
  /** @type {[boolean, Function]} Speichert, ob der Replay vor dem Anzeigen eines Signals aktiv war. */
  const [wasPlayingBeforeSignal, setWasPlayingBeforeSignal] = useState(false);


  /** @type {number} Der maximale Index der Marktdaten. */
  const maxReplayIndex = marketData.length - 1;
  /** @type {React.MutableRefObject<NodeJS.Timeout|null>} Referenz auf den Timeout-Timer für das automatische Abspielen. */
  const timerRef = useRef(null);

  /**
   * Aktualisiert das `replaySignal` für einen gegebenen Index.
   * @param {number} index - Der Index der Kerze, die evaluiert werden soll.
   * @returns {Promise<object|void>} Das Ergebnis der Strategie-Evaluierung.
   */
  const updateSignalAt = useCallback(async (index) => {
    if (!marketData || marketData.length === 0) return;
    setReplayLoading(true);
    const result = await evaluateDecisionAtIndex(marketData, index);
    setReplaySignal(result);
    setReplayLoading(false);
    return result;
  }, [evaluateDecisionAtIndex, marketData]);

  /**
   * Effekt zur Initialisierung und zum Zurücksetzen des Replay-Modus.
   * - Beim Aktivieren des Replays wird die erste Kerze evaluiert.
   * - Beim Deaktivieren des Replays werden alle Replay-spezifischen Zustände zurückgesetzt.
   */
  useEffect(() => {
    if (replayEnabled && !replaySignal && marketData.length > 0) {
      // Wenn Replay aktiviert wird und noch kein Signal evaluiert wurde, starte bei replayIndex.
      updateSignalAt(replayIndex);
    }
    if (!replayEnabled) {
      // Wenn Replay deaktiviert wird, setze den Signalzustand zurück und stoppe das Abspielen.
      setReplaySignal(null);
      setIsPlaying(false);
      setWasPlayingBeforeSignal(false); // Auch diesen Zustand zurücksetzen
    }
  }, [replayEnabled, replayIndex, marketData, replaySignal, updateSignalAt]);

  /**
   * Bewegt den Replay-Index um einen bestimmten Delta-Wert und aktualisiert das Signal.
   * @param {number} delta - Die Anzahl der Schritte, um die der Index geändert werden soll (z.B. 1 für vorwärts, -1 für zurück).
   */
  const handleReplayStep = useCallback(async (delta) => {
    // Berechnet den nächsten gültigen Index.
    const next = Math.max(0, Math.min(maxReplayIndex, replayIndex + delta));
    if (next === replayIndex) return; // Nichts tun, wenn der Index sich nicht ändert.
    setReplayIndex(next);
    await updateSignalAt(next); // Aktualisiert das Signal für den neuen Index.
  }, [maxReplayIndex, replayIndex, updateSignalAt]);

  /**
   * Springt direkt zu einem bestimmten Replay-Index und aktualisiert das Signal.
   * @param {number} index - Der Ziel-Index.
   */
  const goToReplayIndex = useCallback(async (index) => {
    const next = Math.max(0, Math.min(maxReplayIndex, index));
    setReplayIndex(next);
    await updateSignalAt(next);
  }, [maxReplayIndex, updateSignalAt]);

  /**
   * Effekt für das automatische Abspielen.
   * Steuert den Timer und die Logik zum Stoppen bei Handelssignalen.
   */
  useEffect(() => {
    if (isPlaying && replayEnabled && replayIndex < maxReplayIndex) {
      // Setzt einen Timeout für die nächste Kerze.
      timerRef.current = setTimeout(async () => {
        const nextIndex = replayIndex + 1;
        setReplayIndex(nextIndex);
        const result = await updateSignalAt(nextIndex);

        // STOPP-Logik: Wenn ein BUY- oder SELL-Signal erkannt wird, stoppe das Abspielen.
        if (result && result.status === "READY" && (result.decision.signal === "BUY" || result.decision.signal === "SELL")) {
          setWasPlayingBeforeSignal(true); // Merke, dass es vor dem Signal abgespielt wurde
          setIsPlaying(false); // Stoppt das Abspielen.
          setLastTradeSignal({ // Speichert das Signal für die Anzeige im Modal.
            type: result.decision.signal,
            price: result.close,
            reason: result.decision.reason,
            index: nextIndex
          });
        }
      }, playbackSpeed);
    } else {
      // Wenn das Abspielen gestoppt oder beendet ist, Timer löschen.
      // Wichtig: wasPlayingBeforeSignal hier NICHT zurücksetzen, da es für resetTradeSignal benötigt wird.
      clearTimeout(timerRef.current);
    }
    // Cleanup-Funktion: Stellt sicher, dass der Timer beim Unmounten oder vor einem neuen Effekt gelöscht wird.
    return () => clearTimeout(timerRef.current);
  }, [isPlaying, replayEnabled, replayIndex, maxReplayIndex, playbackSpeed, updateSignalAt]);

  /**
   * Schaltet den Wiedergabestatus (Play/Pause) um.
   */
  const togglePlay = () => setIsPlaying(!isPlaying);

  /**
   * Setzt das `lastTradeSignal` zurück, um das Modal auszublenden.
   * Setzt das Abspielen fort, wenn es vor dem Signal aktiv war.
   */
  const resetTradeSignal = () => {
    setLastTradeSignal(null);
    if (wasPlayingBeforeSignal) {
      setIsPlaying(true); // Setzt das Abspielen fort
      setWasPlayingBeforeSignal(false); // Zustand zurücksetzen
    }
  };

  return {
    replayEnabled,
    setReplayEnabled,
    replayIndex,
    setReplayIndex,
    replaySignal,
    setReplaySignal,
    replayMarkers,
    setReplayMarkers,
    replayLoading,
    handleReplayStep,
    goToReplayIndex,
    maxReplayIndex,
    isPlaying,
    togglePlay,
    playbackSpeed,
    setPlaybackSpeed,
    lastTradeSignal,
    resetTradeSignal
  };
}
