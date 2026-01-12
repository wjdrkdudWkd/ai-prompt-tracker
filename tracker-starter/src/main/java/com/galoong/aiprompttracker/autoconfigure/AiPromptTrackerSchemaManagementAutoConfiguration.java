package com.galoong.aiprompttracker.autoconfigure;

import com.galoong.aiprompttracker.config.properties.TrackingFlywayProperties;
import com.galoong.aiprompttracker.config.properties.TrackingPersistenceProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Auto-configuration for detecting and warning about schema management conflicts.
 *
 * <p><b>Purpose:</b>
 * AI Prompt Tracker supports two schema management modes:
 * <ul>
 *   <li><b>Flyway-managed schema mode (Recommended):</b> Flyway migrations control the schema.
 *       Set {@code ai-prompts.tracking.flyway.enabled=true} and {@code spring.jpa.hibernate.ddl-auto=none} (or validate).</li>
 *   <li><b>Hibernate-managed schema mode:</b> Hibernate ddl-auto controls the schema.
 *       Set {@code ai-prompts.tracking.flyway.enabled=false} and {@code spring.jpa.hibernate.ddl-auto=update/create-drop}.</li>
 * </ul>
 *
 * <p><b>Conflict Detection:</b>
 * When Flyway is enabled AND ddl-auto is set to a schema-modifying value (update/create/create-drop),
 * both systems may attempt to manage the schema, causing conflicts like "Index already exists" errors.
 *
 * <p>This configuration detects such conflicts and logs a warning with guidance, but does NOT
 * automatically override user settings.
 *
 * @see TrackingFlywayProperties
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "ai-prompts.tracking.persistence",
        name = "mode",
        havingValue = "jdbc"
)
@AutoConfigureAfter(FlywayAutoConfiguration.class)
public class AiPromptTrackerSchemaManagementAutoConfiguration {

    private static final AtomicBoolean WARNING_EMITTED = new AtomicBoolean(false);

    @Bean
    public ApplicationListener<ApplicationReadyEvent> schemaManagementConflictDetector(
            Environment environment,
            TrackingFlywayProperties flywayProperties,
            TrackingPersistenceProperties persistenceProperties) {

        return event -> {
            // Only check when persistence mode is JDBC
            if (persistenceProperties.getMode() != TrackingPersistenceProperties.PersistenceMode.JDBC) {
                return;
            }

            // Check if Flyway is enabled (either via starter property or Spring Boot property)
            boolean flywayEnabled = flywayProperties.isEnabled()
                    || "true".equalsIgnoreCase(environment.getProperty("spring.flyway.enabled"));

            if (!flywayEnabled) {
                logSchemaManagementMode("Hibernate-managed", environment);
                return;
            }

            // Flyway is enabled - check ddl-auto value
            String ddlAuto = environment.getProperty("spring.jpa.hibernate.ddl-auto");

            if (ddlAuto == null || ddlAuto.isBlank()) {
                // Default value varies by Spring Boot version, but typically "none" with Flyway
                logSchemaManagementMode("Flyway-managed", environment);
                return;
            }

            ddlAuto = ddlAuto.trim().toLowerCase();

            // Safe values that don't modify schema
            if ("none".equals(ddlAuto) || "validate".equals(ddlAuto)) {
                logSchemaManagementMode("Flyway-managed", environment);
                return;
            }

            // Conflict detected: Flyway enabled + schema-modifying ddl-auto
            if (WARNING_EMITTED.compareAndSet(false, true)) {
                logConflictWarning(ddlAuto, flywayEnabled);
            }
        };
    }

    private void logSchemaManagementMode(String mode, Environment environment) {
        String ddlAuto = environment.getProperty("spring.jpa.hibernate.ddl-auto", "default");
        log.info("╔═══════════════════════════════════════════════════════════════════════════════╗");
        log.info("║           AI PROMPT TRACKER - Schema Management Mode: {}           ║", String.format("%-11s", mode));
        log.info("╚═══════════════════════════════════════════════════════════════════════════════╝");
        log.info("  Current configuration:");
        log.info("    - spring.jpa.hibernate.ddl-auto: {}", ddlAuto);
        log.info("    - Schema managed by: {}", mode.replace("-managed", ""));
        log.info("");
    }

    private void logConflictWarning(String ddlAuto, boolean flywayEnabled) {
        log.warn("╔═══════════════════════════════════════════════════════════════════════════════╗");
        log.warn("║                  ⚠️  SCHEMA MANAGEMENT CONFLICT DETECTED  ⚠️                   ║");
        log.warn("╚═══════════════════════════════════════════════════════════════════════════════╝");
        log.warn("");
        log.warn("Current configuration:");
        log.warn("  - Flyway enabled: {}", flywayEnabled);
        log.warn("  - spring.jpa.hibernate.ddl-auto: {}", ddlAuto);
        log.warn("");
        log.warn("⚠️  ISSUE:");
        log.warn("  Both Flyway and Hibernate are configured to manage the database schema.");
        log.warn("  This can cause conflicts such as:");
        log.warn("    - \"Index 'XXX' already exists\" errors");
        log.warn("    - \"Table already exists\" errors");
        log.warn("    - Unpredictable schema state");
        log.warn("");
        log.warn("📋 RECOMMENDED SOLUTIONS:");
        log.warn("");
        log.warn("Option 1️⃣: Use Flyway-managed schema mode (Recommended for production)");
        log.warn("  application.yml:");
        log.warn("    spring:");
        log.warn("      jpa:");
        log.warn("        hibernate:");
        log.warn("          ddl-auto: none  # or 'validate'");
        log.warn("    ai-prompts:");
        log.warn("      tracking:");
        log.warn("        flyway:");
        log.warn("          enabled: true");
        log.warn("");
        log.warn("Option 2️⃣: Use Hibernate-managed schema mode (For development/testing)");
        log.warn("  application.yml:");
        log.warn("    spring:");
        log.warn("      jpa:");
        log.warn("        hibernate:");
        log.warn("          ddl-auto: {}  # Keep your current value", ddlAuto);
        log.warn("      flyway:");
        log.warn("        enabled: false");
        log.warn("    ai-prompts:");
        log.warn("      tracking:");
        log.warn("        flyway:");
        log.warn("          enabled: false");
        log.warn("");
        log.warn("⚠️  Note: AI Prompt Tracker will NOT automatically change your settings.");
        log.warn("   Please choose one of the options above and update your configuration.");
        log.warn("");
        log.warn("╚═══════════════════════════════════════════════════════════════════════════════╝");
    }
}
