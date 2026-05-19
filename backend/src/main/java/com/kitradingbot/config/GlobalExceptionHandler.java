package com.kitradingbot.config;

import com.kitradingbot.model.ApiError;
import com.kitradingbot.service.TradingBotException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Zentraler Fehlerbehandler für die gesamte REST-API.
 * Fängt Ausnahmen ab und wandelt sie in ein einheitliches JSON-Format ({@link ApiError}) um.
 * Dies verhindert, dass interne Serverfehler oder unerwartete Ausnahmen direkt an den Client gesendet werden
 * und sorgt für eine konsistente Fehlerantwort.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Behandelt spezifische Geschäftslogik-Fehler, die von {@link TradingBotException} ausgelöst werden.
     * Diese Fehler werden als HTTP 400 Bad Request zurückgegeben.
     *
     * @param ex Die ausgelöste {@link TradingBotException}.
     * @return Eine {@link ResponseEntity} mit {@link ApiError} und HTTP-Status 400.
     */
    @ExceptionHandler(TradingBotException.class)
    public ResponseEntity<ApiError> handleTradingBotException(TradingBotException ex) {
        log.warn("Behandelte TradingBot-Ausnahme: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(new ApiError(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage()
        ));
    }

    /**
     * Behandelt Validierungsfehler, die bei der Verarbeitung von Request-Parametern oder Body-Objekten auftreten
     * (z.B. durch `@Valid` oder `@Validated` Annotationen).
     * Die Fehlermeldungen der einzelnen Felder werden gesammelt und als HTTP 400 Bad Request zurückgegeben.
     *
     * @param ex Die ausgelöste {@link MethodArgumentNotValidException}.
     * @return Eine {@link ResponseEntity} mit {@link ApiError} und HTTP-Status 400.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(new ApiError(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                message
        ));
    }

    /**
     * Fängt alle anderen unerwarteten Ausnahmen ab, die nicht spezifisch behandelt wurden.
     * Dies verhindert, dass interne Stacktraces an den Client gelangen und gibt stattdessen
     * eine generische Fehlermeldung mit HTTP-Status 500 Internal Server Error zurück.
     *
     * @param ex Die ausgelöste {@link Exception}.
     * @return Eine {@link ResponseEntity} mit {@link ApiError} und HTTP-Status 500.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        log.error("Unerwarteter Fehler", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiError(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Interner Serverfehler"
        ));
    }
}
