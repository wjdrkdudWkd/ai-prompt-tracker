package com.galoong.aiprompttracker.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for AI Prompt Tracker Flyway migrations.
 *
 * <p>Controls whether the library auto-runs database migrations.
 * Default is {@code false} to avoid surprising users in production.
 */
@Data
@ConfigurationProperties(prefix = "ai-prompts.tracking.flyway")
public class TrackingFlywayProperties {

    /**
     * Enable Flyway migrations for AI Prompt Tracker schema.
     *
     * <p>Default: {@code false}
     *
     * <p>When {@code true}, the library will auto-run Flyway migrations from
     * {@code classpath:db/migration/tracking/} to create/update the executions
     * and calls tables.
     *
     * <p>When {@code false} (default), the library will NOT run migrations.
     * User must either:
     * <ul>
     *   <li>Set this to {@code true} to enable auto-migrations, OR</li>
     *   <li>Manually run migrations via their own Flyway setup, OR</li>
     *   <li>Create tables manually (see schema in migration files)</li>
     * </ul>
     *
     * <p><b>Production recommendation:</b> Keep this {@code false} and manage
     * migrations via your own Flyway setup or DB deployment pipeline.
     */
    private boolean enabled = false;
}
