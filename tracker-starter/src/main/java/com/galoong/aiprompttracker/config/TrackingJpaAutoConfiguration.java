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

/**
 * Auto-configuration for JPA-based persistence.
 *
 * <p>This configuration is isolated to prevent ClassNotFoundException
 * when JPA is not on the classpath.
 */
@Slf4j
@Configuration
@ConditionalOnClass(name = "jakarta.persistence.Entity")
@ConditionalOnProperty(
        name = "ai-prompts.tracking.persistence.mode",
        havingValue = "jdbc"
)
@EnableJpaRepositories(basePackages = "com.galoong.aiprompttracker.domain.repository")
@EntityScan(basePackages = "com.galoong.aiprompttracker.domain.entity")
public class TrackingJpaAutoConfiguration {

    /**
     * JDBC mode: JPA execution store
     */
    @Bean
    public ExecutionStore jpaExecutionStore(ExecutionRepository executionRepository) {
        log.info("Persistence mode: JDBC - enabling JPA execution store");
        return new JpaExecutionStore(executionRepository);
    }

    /**
     * JDBC mode: JPA call store
     */
    @Bean
    public CallStore jpaCallStore(CallRepository callRepository) {
        log.info("Persistence mode: JDBC - enabling JPA call store");
        return new JpaCallStore(callRepository);
    }
}
