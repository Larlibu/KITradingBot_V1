package com.kitradingbot.model;

import java.time.LocalDateTime;

/**
 * Repräsentiert eine standardisierte Fehlermeldung, die von der REST-API zurückgegeben wird.
 * Dieses Record-Objekt wird verwendet, um konsistente Fehlerantworten an den Client zu senden.
 *
 * @param timestamp Der Zeitpunkt, zu dem der Fehler aufgetreten ist.
 * @param status    Der HTTP-Statuscode des Fehlers (z.B. 400 für Bad Request, 500 für Internal Server Error).
 * @param message   Eine detaillierte Beschreibung des Fehlers.
 */
public record ApiError(LocalDateTime timestamp, int status, String message) {
}
