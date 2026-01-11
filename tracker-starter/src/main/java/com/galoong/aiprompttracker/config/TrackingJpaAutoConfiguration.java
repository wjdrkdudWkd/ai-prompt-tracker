package com.galoong.aiprompttracker.config;

import com.galoong.aiprompttracker.domain.repository.CallRepository;
import com.galoong.aiprompttracker.domain.repository.ExecutionRepository;
import com.galoong.aiprompttracker.tracking.storage.CallStore;
import com.galoong.aiprompttracker.tracking.storage.ExecutionStore;
import com.galoong.aiprompttracker.tracking.storage.JpaCallStore;
import com.galoong.aiprompttracker.tracking.storage.JpaExecutionStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.persistence.EntityManager;

/**
 * Auto-configuration for JPA-based persistence.
 *
 * <p><b>Automatic Repository Discovery:</b>
 * This configuration works seamlessly with Spring Boot's default JPA repository scanning.
 * The starter registers its base package via {@link AiPromptTrackerAutoConfigPackageRegistrar},
 * which extends (rather than replaces) Spring Boot's AutoConfigurationPackages.
 *
 * <p><b>Strategy:</b>
 * <ul>
 *   <li>Uses @EntityScan to register our entities</li>
 *   <li>Does NOT use @EnableJpaRepositories (avoids breaking consumer repository scanning)</li>
 *   <li>Repositories are auto-discovered via AutoConfigurationPackages registration</li>
 *   <li>Loads AFTER JpaRepositoriesAutoConfiguration to ensure proper initialization order</li>
 * </ul>
 *
 * <p><b>Consumer Experience - Plug and Play:</b>
 * No consumer-side configuration required. Simply add the starter dependency:
 * <pre>
 * dependencies {
 *     implementation("com.galoong:ai-prompt-tracker-starter:X.Y.Z")
 * }
 * </pre>
 *
 * <p>Both consumer and starter repositories are automatically discovered:
 * <ul>
 *   <li>Consumer repositories (in application base package) - discovered by Boot's default scanning</li>
 *   <li>Starter repositories (com.galoong.aiprompttracker.domain.repository) - discovered via AutoConfigurationPackages</li>
 * </ul>
 *
 * <p><b>Why This Approach:</b>
 * Using @EnableJpaRepositories in a starter causes Spring Boot's JpaRepositoriesAutoConfiguration
 * to back off completely, breaking consumer repository scanning. The AutoConfigurationPackages
 * approach extends Boot's scanning to include additional packages without disrupting the default behavior.
 *
 * @see AiPromptTrackerAutoConfigPackageRegistrar
 * @see org.springframework.boot.autoconfigure.AutoConfigurationPackages
 */
@Slf4j
@Configuration
@ConditionalOnClass(EntityManager.class)
@AutoConfigureAfter(JpaRepositoriesAutoConfiguration.class)
@EntityScan(basePackages = "com.galoong.aiprompttracker.domain.entity")
public class TrackingJpaAutoConfiguration {

    public TrackingJpaAutoConfiguration() {
        log.info("AI Prompt Tracker: JPA entities registered. Repositories will be auto-discovered via AutoConfigurationPackages.");
    }

    /**
     * JDBC mode: JPA execution store
     *
     * <p>Only created when persistence mode is explicitly set to "jdbc"
     */
    @Bean
    @ConditionalOnProperty(
            name = "ai-prompts.tracking.persistence.mode",
            havingValue = "jdbc"
    )
    public ExecutionStore jpaExecutionStore(ExecutionRepository executionRepository) {
        log.info("Persistence mode: JDBC - enabling JPA execution store");
        return new JpaExecutionStore(executionRepository);
    }

    /**
     * JDBC mode: JPA call store
     *
     * <p>Only created when persistence mode is explicitly set to "jdbc"
     */
    @Bean
    @ConditionalOnProperty(
            name = "ai-prompts.tracking.persistence.mode",
            havingValue = "jdbc"
    )
    public CallStore jpaCallStore(CallRepository callRepository) {
        log.info("Persistence mode: JDBC - enabling JPA call store");
        return new JpaCallStore(callRepository);
    }
}
