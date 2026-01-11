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
 * <p><b>Safe JPA Entity and Repository Auto-Discovery:</b>
 * This configuration imports {@link AiPromptTrackerJpaScanAutoConfiguration} which registers a
 * {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor} as a static @Bean.
 * The BFPP safely appends the starter's base package to BOTH:
 * <ul>
 *   <li><b>AutoConfigurationPackages</b> - for repository scanning by Spring Data JPA</li>
 *   <li><b>EntityScanPackages</b> - for entity scanning by Hibernate JPA</li>
 * </ul>
 *
 * <p><b>Dual-Registry Strategy Benefits:</b>
 * <ul>
 *   <li><b>Never creates registries</b> - only appends when registry already exists</li>
 *   <li><b>Never replaces consumer packages</b> - pure append-only operation</li>
 *   <li><b>Ordering-independent</b> - static @Bean ensures early execution</li>
 *   <li><b>Graceful degradation</b> - skips if registry not available</li>
 *   <li><b>Never fails consumer startup</b> - all operations wrapped in try-catch</li>
 * </ul>
 *
 * <p>This allows both consumer and starter entities/repositories to be discovered automatically
 * without requiring {@code @EnableJpaRepositories} or {@code @EntityScan} configuration.
 *
 * <p><b>Troubleshooting:</b>
 * Enable diagnostic mode with {@code ai-prompts.debug.scan=true} to see detailed package
 * registration and entity discovery information.
 *
 * @see AiPromptTrackerJpaScanAutoConfiguration
 * @see AiPromptTrackerJpaDiagnosticsAutoConfiguration
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
    AiPromptTrackerJpaScanAutoConfiguration.class,      // FIRST: Register packages for JPA scanning
    AiPromptTrackerJpaDiagnosticsAutoConfiguration.class, // Diagnostic mode (conditional)
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
