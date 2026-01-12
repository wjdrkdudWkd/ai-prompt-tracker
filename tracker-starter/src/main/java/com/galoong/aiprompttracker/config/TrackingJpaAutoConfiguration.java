package com.galoong.aiprompttracker.config;

import com.galoong.aiprompttracker.domain.repository.CallRepository;
import com.galoong.aiprompttracker.domain.repository.ExecutionRepository;
import com.galoong.aiprompttracker.tracking.storage.CallStore;
import com.galoong.aiprompttracker.tracking.storage.ExecutionStore;
import com.galoong.aiprompttracker.tracking.storage.JpaCallStore;
import com.galoong.aiprompttracker.tracking.storage.JpaExecutionStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.persistence.EntityManager;

/**
 * Auto-configuration for JPA-based persistence.
 *
 * <p><b>Automatic Entity and Repository Discovery:</b>
 * This configuration works seamlessly with Spring Boot's default JPA entity/repository scanning.
 * The starter registers its base package via {@link com.galoong.aiprompttracker.autoconfigure.AiPromptTrackerJpaScanAutoConfiguration},
 * which uses an {@link org.springframework.context.annotation.ImportBeanDefinitionRegistrar} to extend
 * AutoConfigurationPackages EARLY (before repository scanning).
 *
 * <p><b>Strategy - AutoConfigurationPackages Extension via ImportBeanDefinitionRegistrar:</b>
 * <ul>
 *   <li><b>Timing</b>: ImportBeanDefinitionRegistrar runs EARLY during @Configuration class processing</li>
 *   <li><b>Before scanning</b>: Executes BEFORE Spring Data JPA repository scanning begins</li>
 *   <li><b>Append-only</b>: Appends starter package to existing AutoConfigurationPackages</li>
 *   <li>Does NOT use @EntityScan (would create EntityScanPackages and break consumer entity scanning)</li>
 *   <li>Does NOT use @EnableJpaRepositories (would disable Boot's repository auto-configuration)</li>
 * </ul>
 *
 * <p><b>How Scanning Works:</b>
 * <ol>
 *   <li>Consumer's @SpringBootApplication registers consumer package in AutoConfigurationPackages</li>
 *   <li>Our ImportBeanDefinitionRegistrar appends starter package to AutoConfigurationPackages (EARLY)</li>
 *   <li>Spring Data JPA scans AutoConfigurationPackages → discovers all repositories</li>
 *   <li>Hibernate scans AutoConfigurationPackages (when EntityScanPackages not set) → discovers all entities</li>
 * </ol>
 *
 * <p><b>Consumer Experience - Zero Configuration:</b>
 * No consumer-side configuration required. Simply add the starter dependency:
 * <pre>
 * dependencies {
 *     implementation("com.galoong:ai-prompt-tracker-starter:X.Y.Z")
 * }
 * </pre>
 *
 * <p>Both consumer and starter entities/repositories are automatically discovered.
 *
 * <p><b>Consumer with explicit @EntityScan:</b>
 * If consumer uses @EntityScan, they create EntityScanPackages which overrides AutoConfigurationPackages
 * for entity scanning. In this case, consumer MUST include the starter entity package:
 * <pre>
 * {@code @SpringBootApplication}
 * {@code @EntityScan(basePackages = {}
 *     "com.yourcompany.yourapp.domain",
 *     "com.galoong.aiprompttracker.domain.entity"  // Must add this!
 * })
 * </pre>
 * See {@link com.galoong.aiprompttracker.autoconfigure.AiPromptTrackerJpaConsumerWarningsAutoConfiguration}
 * for runtime detection and warnings.
 *
 * <p><b>Why This Approach is Safe:</b>
 * <ul>
 *   <li>Runs EARLY enough to affect repository scanning (ImportBeanDefinitionRegistrar)</li>
 *   <li>Never uses @EntityScan (avoids creating EntityScanPackages)</li>
 *   <li>Never uses @EnableJpaRepositories (avoids disabling repository auto-configuration)</li>
 *   <li>Extends Boot's scanning mechanism without replacement</li>
 *   <li>Consumer entities/repositories guaranteed to work</li>
 * </ul>
 *
 * @see com.galoong.aiprompttracker.autoconfigure.AiPromptTrackerJpaScanAutoConfiguration
 * @see com.galoong.aiprompttracker.autoconfigure.AiPromptTrackerAutoConfigPackageRegistrar
 * @see com.galoong.aiprompttracker.autoconfigure.AiPromptTrackerJpaConsumerWarningsAutoConfiguration
 * @see org.springframework.boot.autoconfigure.AutoConfigurationPackages
 * @see org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration
 * @see org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
 */
@Slf4j
@Configuration
@ConditionalOnClass(EntityManager.class)
@AutoConfigureAfter(JpaRepositoriesAutoConfiguration.class)
public class TrackingJpaAutoConfiguration {

    public TrackingJpaAutoConfiguration() {
        log.info("AI Prompt Tracker: JPA auto-configuration loaded. " +
                "Entities and repositories will be auto-discovered via AutoConfigurationPackages extension.");
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
