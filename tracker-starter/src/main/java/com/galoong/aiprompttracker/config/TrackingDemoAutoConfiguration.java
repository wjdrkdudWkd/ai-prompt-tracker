package com.galoong.aiprompttracker.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Auto-configuration for AI Prompt Tracker demo mode.
 *
 * <p>When {@code ai-prompts.tracking.demo.enabled=true}, this configuration:
 * <ul>
 *   <li>Auto-configures H2 in-memory datasource (if no datasource exists)</li>
 *   <li>Implicitly sets {@code persistence.mode=jdbc}</li>
 *   <li>Enables Flyway migrations via {@code TrackingFlywayAutoConfiguration}</li>
 * </ul>
 *
 * <p><b>WARNING:</b> Demo mode is for local development only. Data is lost on restart.
 *
 * <p>For production, use {@code persistence.mode=jdbc} with a real database.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(
        name = "ai-prompts.tracking.demo.enabled",
        havingValue = "true"
)
public class TrackingDemoAutoConfiguration {

    /**
     * Demo mode datasource: H2 in-memory
     *
     * <p>Only created if no other datasource is configured.
     */
    @Bean
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource demoDataSource() {
        log.warn("=".repeat(80));
        log.warn("AI PROMPT TRACKER: DEMO MODE ENABLED");
        log.warn("Using H2 in-memory database. Data will be lost on restart.");
        log.warn("For production, set ai-prompts.tracking.persistence.mode=jdbc with a real database.");
        log.warn("=".repeat(80));

        return DataSourceBuilder.create()
                .driverClassName("org.h2.Driver")
                .url("jdbc:h2:mem:ai-prompt-tracker;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
                .username("sa")
                .password("")
                .build();
    }
}
