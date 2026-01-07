package com.galoong.aiprompttracker.api.config;

import com.galoong.aiprompttracker.api.exception.PersistenceDisabledException;
import com.galoong.aiprompttracker.tracking.storage.ExecutionStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Auto-configuration for AI Prompt Tracker REST API.
 *
 * <p>When persistence is disabled, API beans are replaced with no-op
 * implementations that throw {@link PersistenceDisabledException}.
 */
@Slf4j
@Configuration
@ComponentScan(basePackages = {
    "com.galoong.aiprompttracker.api.controller",
    "com.galoong.aiprompttracker.api.service"
})
public class ApiAutoConfiguration implements WebMvcConfigurer {

    /**
     * Persistence guard bean
     *
     * <p>Injected into services to check if persistence is enabled.
     */
    @Bean
    public PersistenceGuard persistenceGuard(ExecutionStore executionStore) {
        boolean persistent = executionStore.isPersistent();
        log.info("API persistence guard initialized: persistent={}", persistent);
        return new PersistenceGuard(persistent);
    }

    /**
     * Helper class to guard API operations
     */
    public static class PersistenceGuard {
        private final boolean persistent;

        public PersistenceGuard(boolean persistent) {
            this.persistent = persistent;
        }

        /**
         * Check if persistence is enabled, throw exception if not
         */
        public void requirePersistence() {
            if (!persistent) {
                throw new PersistenceDisabledException();
            }
        }

        public boolean isPersistent() {
            return persistent;
        }
    }
}
