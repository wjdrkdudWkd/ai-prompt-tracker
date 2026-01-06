package com.galoong.aiprompttracker.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for AI Prompt Tracker demo mode.
 *
 * <p>Demo mode provides a zero-config local experience with H2 database.
 * Intended for development, testing, and demonstrations only.
 */
@Data
@ConfigurationProperties(prefix = "ai-prompts.tracking.demo")
public class TrackingDemoProperties {

    /**
     * Enable demo mode with H2 in-memory database.
     *
     * <p>Default: {@code false}
     *
     * <p>When {@code true}, the library will:
     * <ul>
     *   <li>Auto-configure H2 in-memory datasource (if no datasource exists)</li>
     *   <li>Enable JPA/Hibernate with {@code ddl-auto=create-drop}</li>
     *   <li>Set {@code persistence.mode=jdbc} implicitly</li>
     *   <li>Enable Flyway migrations implicitly</li>
     *   <li>Enable H2 console at {@code /h2-console} (if H2 is on classpath)</li>
     * </ul>
     *
     * <p><b>Usage:</b>
     * <pre>
     * # application.yml (local dev only!)
     * ai-prompts:
     *   tracking:
     *     demo:
     *       enabled: true
     * </pre>
     *
     * <p><b>WARNING:</b> Demo mode is NOT for production. Data is lost on restart.
     * For production, use {@code persistence.mode=jdbc} with a real database.
     */
    private boolean enabled = false;
}
