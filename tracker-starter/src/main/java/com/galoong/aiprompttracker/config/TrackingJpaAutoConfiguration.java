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
 * The starter registers its base package via {@link AiPromptTrackerAutoConfigPackageRegistrar},
 * which extends (rather than replaces) Spring Boot's AutoConfigurationPackages.
 *
 * <p><b>Strategy - Pure AutoConfigurationPackages Extension:</b>
 * <ul>
 *   <li>Does NOT use @EntityScan (would narrow scanning and potentially break consumer entities)</li>
 *   <li>Does NOT use @EnableJpaRepositories (would disable Boot's auto-scanning)</li>
 *   <li>Relies ONLY on AutoConfigurationPackages extension for both entities and repositories</li>
 *   <li>Loads AFTER JpaRepositoriesAutoConfiguration to ensure proper initialization order</li>
 * </ul>
 *
 * <p><b>How Entity Scanning Works:</b>
 * Spring Boot's {@code EntityManagerFactoryBuilder} scans all packages in AutoConfigurationPackages.
 * By appending our package, both consumer and starter entities are discovered automatically:
 * <ol>
 *   <li>Consumer's base package (registered by @SpringBootApplication)</li>
 *   <li>Starter's base package (appended by AiPromptTrackerAutoConfigPackageRegistrar)</li>
 *   <li>Both scanned for @Entity classes</li>
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
 * <p>Both consumer and starter entities/repositories are automatically discovered:
 * <ul>
 *   <li>Consumer entities/repositories - discovered by Boot's default scanning</li>
 *   <li>Starter entities/repositories - discovered via AutoConfigurationPackages extension</li>
 * </ul>
 *
 * <p><b>Why This Approach is Safe:</b>
 * <ul>
 *   <li>Never uses @EntityScan (avoids narrowing entity scanning)</li>
 *   <li>Never uses @EnableJpaRepositories (avoids disabling repository auto-configuration)</li>
 *   <li>Extends Boot's scanning mechanism without replacement</li>
 *   <li>Consumer entities/repositories guaranteed to work</li>
 * </ul>
 *
 * @see AiPromptTrackerAutoConfigPackageRegistrar
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
