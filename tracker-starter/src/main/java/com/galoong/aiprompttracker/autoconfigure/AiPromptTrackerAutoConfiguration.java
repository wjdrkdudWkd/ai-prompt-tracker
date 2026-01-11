package com.galoong.aiprompttracker.autoconfigure;

import com.galoong.aiprompttracker.api.config.ApiAutoConfiguration;
import com.galoong.aiprompttracker.config.*;
import com.galoong.aiprompttracker.config.properties.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * Main auto-configuration for AI Prompt Tracker Spring Boot Starter.
 *
 * <p>Enables automatic tracking of AI API calls with @AIPrompt annotation.
 * Provides embedded dashboard UI at /aiprompt-tracker
 *
 * <p><b>Repository Auto-Discovery:</b>
 * This configuration imports {@link AiPromptTrackerAutoConfigPackageRegistrar} which registers
 * the starter's base package into Spring Boot's {@code AutoConfigurationPackages}. This allows
 * Spring Boot's default JPA repository scanning to automatically discover the starter's
 * repositories without requiring consumer-side {@code @EnableJpaRepositories} configuration.
 */
@Slf4j
@AutoConfiguration
@ConditionalOnWebApplication
@EnableConfigurationProperties({
    TrackingCaptureProperties.class,
    TrackingPersistenceProperties.class,
    TrackingFlywayProperties.class,
    TrackingDemoProperties.class
})
@Import({
    AiPromptTrackerAutoConfigPackageRegistrar.class,  // Register base package for repository scanning
    TrackingCoreAutoConfiguration.class,
    TrackingOkHttpAutoConfiguration.class,
    TrackingPersistenceAutoConfiguration.class,
    TrackingJpaAutoConfiguration.class,
    TrackingDemoAutoConfiguration.class,
    TrackingFlywayAutoConfiguration.class,
    ApiAutoConfiguration.class,
    AiPromptTrackerWebMvcConfiguration.class
})
public class AiPromptTrackerAutoConfiguration {

    public AiPromptTrackerAutoConfiguration() {
        log.info("═══════════════════════════════════════════════════════════");
        log.info("  AI Prompt Tracker - Spring Boot Starter");
        log.info("  Dashboard UI: http://localhost:8080/aiprompt-tracker");
        log.info("  API Endpoints: http://localhost:8080/aiprompt-tracker/api");
        log.info("═══════════════════════════════════════════════════════════");
    }
}
