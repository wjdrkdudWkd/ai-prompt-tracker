package com.galoong.aiprompttracker.config;

import com.galoong.aiprompttracker.config.properties.TrackingPersistenceProperties;
import com.galoong.aiprompttracker.tracking.storage.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for AI Prompt Tracker persistence layer.
 *
 * <p>Provides default no-op stores when persistence is disabled.
 * JPA-based stores are configured in {@link TrackingJpaAutoConfiguration}.
 *
 * <p>This configuration is separate from {@link TrackingCoreAutoConfiguration}
 * to ensure tracking works even without a database.
 */
@Slf4j
@Configuration
public class TrackingPersistenceAutoConfiguration {

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
