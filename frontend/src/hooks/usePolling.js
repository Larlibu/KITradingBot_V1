import { useEffect } from "react";

/**
 * Custom Hook zur Implementierung eines Polling-Mechanismus.
 * Führt eine Callback-Funktion in einem bestimmten Intervall aus,
 * solange das Polling aktiviert ist.
 *
 * @param {Function} callback - Die Funktion, die in jedem Intervall ausgeführt werden soll.
 * @param {number} intervalMs - Das Intervall in Millisekunden, in dem der Callback ausgeführt wird.
 * @param {boolean} [enabled=true] - Ein Flag, das steuert, ob das Polling aktiv ist.
 */
export function usePolling(callback, intervalMs, enabled = true) {
  useEffect(() => {
    // Wenn Polling nicht aktiviert ist, wird der Effekt nicht ausgeführt.
    if (!enabled) {
      return undefined; // Keine Cleanup-Funktion nötig, da kein Timer gestartet wurde.
    }

    // Führt den Callback sofort beim Start aus.
    callback();

    // Startet den Intervall-Timer.
    const id = setInterval(callback, intervalMs);

    // Cleanup-Funktion: Wird ausgeführt, wenn die Komponente unmounted wird
    // oder wenn sich die Dependencies ändern und der Effekt neu ausgeführt wird.
    return () => clearInterval(id); // Stoppt den Timer, um Memory Leaks zu vermeiden.
  }, [callback, intervalMs, enabled]); // Dependencies des Hooks.
}
