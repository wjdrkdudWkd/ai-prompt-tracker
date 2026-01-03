package com.galoong.aiprompttracker.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for AI Prompt Tracker persistence modes.
 *
 * <p>Controls how tracking data is persisted (if at all):
 * <ul>
 *   <li><b>none</b> (default): No persistence, tracking runs in-memory only</li>
 *   <li><b>jdbc</b>: Persist to user-provided database via JPA</li>
 * </ul>
 *
 * <p>Example configurations:
 *
 * <pre>
 * # Default: no persistence required
 * ai-prompts.tracking.persistence.mode: none
 *
 * # Demo mode: H2 + auto-migrations
 * ai-prompts.tracking.demo.enabled: true
 *
 * # Production: user database
 * ai-prompts.tracking.persistence.mode: jdbc
 * ai-prompts.tracking.flyway.enabled: false  # or true if you want auto-migrations
 * spring.datasource.url: jdbc:postgresql://localhost:5432/mydb
 * </pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai-prompts.tracking.persistence")
public class TrackingPersistenceProperties {

    /**
     * Persistence mode for tracking data.
     *
     * <p>Options:
     * <ul>
     *   <li><b>none</b> (default): No database persistence. Tracking still works
     *       (AOP, interceptors, metrics) but data is not saved. REST APIs return
     *       503 "persistence disabled" errors.</li>
     *   <li><b>jdbc</b>: Persist to relational database via Spring Data JPA.
     *       Requires user to configure datasource. Flyway migrations are opt-in
     *       via {@code ai-prompts.tracking.flyway.enabled}.</li>
     * </ul>
     */
    private PersistenceMode mode = PersistenceMode.NONE;

    /**
     * Persistence mode enum
     */
    public enum PersistenceMode {
        /**
         * No persistence (default). Tracking runs in-memory only.
         * REST APIs return 503 Service Unavailable.
         */
        NONE,

        /**
         * Persist to JDBC database via JPA repositories.
         * Requires user-configured datasource.
         */
        JDBC
    }
}
