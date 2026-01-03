package com.galoong.aiprompttracker.api.exception;

/**
 * Exception thrown when API endpoints are called but persistence is disabled.
 *
 * <p>Thrown by controllers when {@code ai-prompts.tracking.persistence.mode=none}.
 */
public class PersistenceDisabledException extends RuntimeException {

    public PersistenceDisabledException() {
        super("AI Prompt Tracker persistence is disabled. " +
                "Enable via ai-prompts.tracking.persistence.mode=jdbc or ai-prompts.tracking.demo.enabled=true");
    }

    public PersistenceDisabledException(String message) {
        super(message);
    }
}
