package com.galoong.aiprompttracker.config;

import com.galoong.aiprompttracker.domain.repository.CallRepository;
import com.galoong.aiprompttracker.domain.repository.ExecutionRepository;
import com.galoong.aiprompttracker.tracking.storage.CallStore;
import com.galoong.aiprompttracker.tracking.storage.ExecutionStore;
import com.galoong.aiprompttracker.tracking.storage.JpaCallStore;
import com.galoong.aiprompttracker.tracking.storage.JpaExecutionStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import jakarta.persistence.EntityManager;

/**
 * Auto-configuration for JPA-based persistence.
 *
 * <p>This configuration automatically registers JPA entities and repositories
 * when JPA is on the classpath, following Spring Boot Starter best practices.
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li>Automatically enables entity scanning for com.galoong.aiprompttracker.domain</li>
 *   <li>Automatically enables repository scanning for com.galoong.aiprompttracker.domain.repository</li>
 *   <li>Only activates when JPA (EntityManager) is available</li>
 *   <li>No consumer-side configuration required (@EnableJpaRepositories, @EntityScan)</li>
 * </ul>
 */
@Slf4j
@Configuration
@ConditionalOnClass(EntityManager.class)
@EnableJpaRepositories(basePackages = "com.galoong.aiprompttracker.domain.repository")
@EntityScan(basePackages = "com.galoong.aiprompttracker.domain.entity")
public class TrackingJpaAutoConfiguration {

    public TrackingJpaAutoConfiguration() {
        log.info("AI Prompt Tracker: JPA entities and repositories auto-registered");
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
