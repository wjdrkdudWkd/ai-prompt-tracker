package com.galoong.aiprompttracker.config;

import com.galoong.aiprompttracker.config.properties.TrackingPersistenceProperties;
import com.galoong.aiprompttracker.domain.repository.CallRepository;
import com.galoong.aiprompttracker.domain.repository.ExecutionRepository;
import com.galoong.aiprompttracker.tracking.storage.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for AI Prompt Tracker persistence layer.
 *
 * <p>Conditionally enables JPA repositories and stores based on
 * {@code ai-prompts.tracking.persistence.mode}:
 *
 * <ul>
 *   <li><b>none</b> (default): No persistence, uses {@link NoopExecutionStore}
 *       and {@link NoopCallStore}</li>
 *   <li><b>jdbc</b>: Enables JPA persistence via {@link JpaExecutionStore}
 *       and {@link JpaCallStore}</li>
 * </ul>
 *
 * <p>This configuration is separate from {@link TrackingCoreAutoConfiguration}
 * to ensure tracking works even without a database.
 */
@Slf4j
@Configuration
public class TrackingPersistenceAutoConfiguration {

    /**
     * JDBC mode: JPA execution store
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

    /**
     * Default: No-op execution store (when persistence disabled)
     */
    @Bean
    @ConditionalOnMissingBean(ExecutionStore.class)
    public ExecutionStore noopExecutionStore() {
        log.info("Persistence mode: NONE - using no-op execution store (data not persisted)");
        return new NoopExecutionStore();
    }

    /**
     * Default: No-op call store (when persistence disabled)
     */
    @Bean
    @ConditionalOnMissingBean(CallStore.class)
    public CallStore noopCallStore() {
        log.info("Persistence mode: NONE - using no-op call store (data not persisted)");
        return new NoopCallStore();
    }
}
