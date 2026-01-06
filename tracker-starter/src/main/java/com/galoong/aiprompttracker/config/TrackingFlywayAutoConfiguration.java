package com.galoong.aiprompttracker.config;

import com.galoong.aiprompttracker.config.properties.TrackingDemoProperties;
import com.galoong.aiprompttracker.config.properties.TrackingFlywayProperties;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Auto-configuration for AI Prompt Tracker Flyway migrations.
 *
 * <p>Flyway migrations are enabled when:
 * <ul>
 *   <li>{@code ai-prompts.tracking.flyway.enabled=true}, OR</li>
 *   <li>{@code ai-prompts.tracking.demo.enabled=true} (demo mode)</li>
 * </ul>
 *
 * <p>Default: Flyway is <b>disabled</b> to avoid surprising users in production.
 *
 * <p>Migrations are located in {@code classpath:db/migration/tracking/}.
 */
@Slf4j
@Configuration
@ConditionalOnClass(Flyway.class)
public class TrackingFlywayAutoConfiguration {

    /**
     * Flyway bean for AI Prompt Tracker migrations
     *
     * <p>Only created if flyway.enabled=true OR demo.enabled=true
     */
    @Bean(initMethod = "migrate")
    public Flyway trackingFlyway(
            DataSource dataSource,
            TrackingFlywayProperties flywayProps,
            TrackingDemoProperties demoProps) {

        boolean enabled = flywayProps.isEnabled() || demoProps.isEnabled();

        if (!enabled) {
            log.debug("AI Prompt Tracker Flyway migrations disabled (flyway.enabled=false, demo.enabled=false)");
            // Return no-op Flyway that does nothing
            return Flyway.configure()
                    .dataSource(dataSource)
                    .load();
        }

        log.info("AI Prompt Tracker Flyway migrations enabled - migrating schema...");

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/tracking")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .table("flyway_tracking_schema_history")
                .load();

        // Migrate will be called by initMethod
        return flyway;
    }
}
