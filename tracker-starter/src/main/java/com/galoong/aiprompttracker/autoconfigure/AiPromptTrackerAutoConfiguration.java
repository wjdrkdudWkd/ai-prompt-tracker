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
 * Enables automatic tracking of AI API calls with @AIPrompt annotation.
 * Provides embedded dashboard UI at /aiprompt-tracker
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
