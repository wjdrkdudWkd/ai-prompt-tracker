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
 * This configuration imports {@link AiPromptTrackerJpaScanAutoConfiguration} which uses an
 * {@link org.springframework.context.annotation.ImportBeanDefinitionRegistrar} to register
 * the starter's base package into {@link org.springframework.boot.autoconfigure.AutoConfigurationPackages}.
 *
 * <p><b>Why ImportBeanDefinitionRegistrar?</b>
 * <ul>
 *   <li><b>Early execution</b> - Runs during @Configuration class processing (BEFORE repository scanning)</li>
 *   <li><b>Not too late</b> - BeanFactoryPostProcessor runs AFTER repository scanning (too late!)</li>
 *   <li><b>Append-only</b> - Appends starter package to existing AutoConfigurationPackages</li>
 *   <li><b>Never replaces</b> - Consumer packages remain intact</li>
 * </ul>
 *
 * <p><b>AutoConfigurationPackages Strategy:</b>
 * <ul>
 *   <li>Consumer's @SpringBootApplication registers consumer package</li>
 *   <li>Starter appends its package to AutoConfigurationPackages</li>
 *   <li>Spring Data JPA scans AutoConfigurationPackages → discovers all repositories</li>
 *   <li>Hibernate scans AutoConfigurationPackages (when EntityScanPackages not set) → discovers all entities</li>
 * </ul>
 *
 * <p><b>Consumer with explicit @EntityScan:</b>
 * If consumer uses @EntityScan, they MUST include the starter entity package.
 * See {@link AiPromptTrackerJpaConsumerWarningsAutoConfiguration} for runtime warnings.
 *
 * <p><b>Troubleshooting:</b>
 * Enable diagnostic mode with {@code ai-prompts.debug.scan=true} to see detailed package
 * registration and entity discovery information.
 *
 * @see AiPromptTrackerJpaScanAutoConfiguration
 * @see AiPromptTrackerJpaConsumerWarningsAutoConfiguration
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
    AiPromptTrackerJpaScanAutoConfiguration.class,           // FIRST: Register packages for JPA scanning (early!)
    AiPromptTrackerJpaConsumerWarningsAutoConfiguration.class, // Warn about explicit @EntityScan issues
    AiPromptTrackerJpaDiagnosticsAutoConfiguration.class,    // Diagnostic mode (conditional)
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
