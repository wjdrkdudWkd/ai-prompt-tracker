package com.galoong.aiprompttracker.api.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Global exception handler for AI Prompt Tracker API.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle PersistenceDisabledException with 503 Service Unavailable
     */
    @ExceptionHandler(PersistenceDisabledException.class)
    public ResponseEntity<Map<String, String>> handlePersistenceDisabled(PersistenceDisabledException ex) {
        log.debug("API call rejected: persistence disabled");

        Map<String, String> error = Map.of(
                "error", "PERSISTENCE_DISABLED",
                "message", ex.getMessage(),
                "howToEnable", "Set ai-prompts.tracking.persistence.mode=jdbc (with datasource) or ai-prompts.tracking.demo.enabled=true"
        );

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(error);
    }
}
