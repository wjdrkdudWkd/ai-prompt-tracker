package com.galoong.aiprompttracker.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScanPackages;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.EntityType;
import java.util.List;
import java.util.Set;

/**
 * Diagnostic auto-configuration for troubleshooting JPA scanning issues.
 *
 * <p><b>Activation:</b>
 * Enable via property: {@code ai-prompts.debug.scan=true}
 *
 * <p><b>What It Does:</b>
 * When enabled, this configuration registers an {@link ApplicationListener} that logs detailed
 * information about JPA entity and repository scanning at application startup:
 * <ul>
 *   <li>AutoConfigurationPackages registered (for repository scanning)</li>
 *   <li>EntityScanPackages registered (for entity scanning)</li>
 *   <li>All JPA entities discovered by Hibernate</li>
 *   <li>Relevant Spring Boot and Hibernate properties</li>
 * </ul>
 *
 * <p><b>Usage Example:</b>
 * <pre>
 * # application.yml
 * ai-prompts:
 *   debug:
 *     scan: true
 * </pre>
 *
 * Then run:
 * <pre>
 * ./gradlew bootRun --args='--debug --logging.level.com.galoong=DEBUG'
 * </pre>
 *
 * <p><b>Expected Output:</b>
 * You should see log messages like:
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                    AI PROMPT TRACKER - JPA SCAN DIAGNOSTICS                   ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * [1] AutoConfigurationPackages (used for repository scanning):
 *     - com.example.yourapp
 *     - com.galoong.aiprompttracker
 *
 * [2] EntityScanPackages (used for entity scanning):
 *     - com.example.yourapp
 *     - com.galoong.aiprompttracker
 *
 * [3] JPA Entities discovered by Hibernate:
 *     ✅ Consumer entities:
 *        - com.example.yourapp.domain.User
 *        - com.example.yourapp.domain.Product
 *     ✅ Starter entities:
 *        - com.galoong.aiprompttracker.domain.entity.Execution
 *        - com.galoong.aiprompttracker.domain.entity.Call
 * </pre>
 *
 * <p><b>Troubleshooting:</b>
 * <ul>
 *   <li>If consumer entities are MISSING: AutoConfigurationPackages or EntityScanPackages is broken</li>
 *   <li>If starter entities are MISSING: Package registration failed - check BFPP logs</li>
 *   <li>If packages look correct but entities missing: Check @Entity annotations and package structure</li>
 * </ul>
 *
 * @see AiPromptTrackerJpaScanAutoConfiguration
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "ai-prompts.debug.scan", havingValue = "true")
@AutoConfigureAfter(HibernateJpaAutoConfiguration.class)
public class AiPromptTrackerJpaDiagnosticsAutoConfiguration {

    @Bean
    public ApplicationListener<ApplicationReadyEvent> jpaScanDiagnosticsListener(
            ConfigurableListableBeanFactory beanFactory,
            Environment environment) {

        return event -> {
            log.info("╔═══════════════════════════════════════════════════════════════════════════════╗");
            log.info("║                    AI PROMPT TRACKER - JPA SCAN DIAGNOSTICS                   ║");
            log.info("╚═══════════════════════════════════════════════════════════════════════════════╝");
            log.info("");

            // [1] AutoConfigurationPackages
            diagnoseAutoConfigurationPackages(beanFactory);

            // [2] EntityScanPackages
            diagnoseEntityScanPackages(beanFactory);

            // [3] Discovered JPA Entities
            diagnoseDiscoveredEntities(beanFactory);

            // [4] Relevant Properties
            diagnoseRelevantProperties(environment);

            log.info("");
            log.info("╔═══════════════════════════════════════════════════════════════════════════════╗");
            log.info("║                         DIAGNOSTICS COMPLETE                                  ║");
            log.info("╚═══════════════════════════════════════════════════════════════════════════════╝");
        };
    }

    private void diagnoseAutoConfigurationPackages(ConfigurableListableBeanFactory beanFactory) {
        log.info("[1] AutoConfigurationPackages (used for BOTH repository AND entity scanning):");

        try {
            if (!AutoConfigurationPackages.has(beanFactory)) {
                log.warn("    ⚠️  AutoConfigurationPackages NOT REGISTERED - This is a CRITICAL issue!");
                log.warn("    → Consumer repositories will NOT be discovered");
                log.warn("    → Consumer entities will NOT be discovered");
                log.warn("    → Starter repositories will NOT be discovered");
                log.warn("    → Starter entities will NOT be discovered");
                return;
            }

            List<String> packages = AutoConfigurationPackages.get(beanFactory);
            if (packages == null || packages.isEmpty()) {
                log.warn("    ⚠️  AutoConfigurationPackages is EMPTY - This is a CRITICAL issue!");
                return;
            }

            log.info("    ✅ Total packages: {}", packages.size());

            // Separate consumer and starter packages
            long consumerCount = packages.stream()
                    .filter(pkg -> !pkg.startsWith("com.galoong.aiprompttracker"))
                    .count();
            long starterCount = packages.stream()
                    .filter(pkg -> pkg.startsWith("com.galoong.aiprompttracker"))
                    .count();

            // Show consumer packages
            if (consumerCount > 0) {
                log.info("    ✅ Consumer packages: {}", consumerCount);
                packages.stream()
                        .filter(pkg -> !pkg.startsWith("com.galoong.aiprompttracker"))
                        .forEach(pkg -> log.info("       - {}", pkg));
            } else {
                log.warn("    ⚠️  NO consumer packages found!");
                log.warn("    → This may indicate:");
                log.warn("       1) Consumer @SpringBootApplication is in same package as starter");
                log.warn("       2) AutoConfigurationPackages registration error");
            }

            log.info("");

            // Show starter packages
            if (starterCount > 0) {
                log.info("    ✅ Starter packages: {}", starterCount);
                packages.stream()
                        .filter(pkg -> pkg.startsWith("com.galoong.aiprompttracker"))
                        .forEach(pkg -> log.info("       - {}", pkg));
            } else {
                log.warn("    ⚠️  Starter package 'com.galoong.aiprompttracker' NOT found!");
                log.warn("    → Starter repositories will NOT be discovered");
                log.warn("    → Starter entities will NOT be discovered");
                log.warn("    → Check AiPromptTrackerJpaScanAutoConfiguration BFPP logs");
            }

        } catch (Exception e) {
            log.error("    ❌ Failed to read AutoConfigurationPackages: {}", e.getMessage(), e);
        }

        log.info("");
    }

    private void diagnoseEntityScanPackages(ConfigurableListableBeanFactory beanFactory) {
        log.info("[2] EntityScanPackages (OPTIONAL - used for entity scanning if set):");

        try {
            EntityScanPackages entityScanPackages = EntityScanPackages.get(beanFactory);
            List<String> packages = entityScanPackages.getPackageNames();

            if (packages == null || packages.isEmpty()) {
                log.warn("    ⚠️  EntityScanPackages is EMPTY");
                log.info("    → Hibernate will fall back to AutoConfigurationPackages");
            } else {
                log.warn("    ⚠️  EntityScanPackages EXISTS - Hibernate will use ONLY these packages:");
                log.warn("    ⚠️  AutoConfigurationPackages will be IGNORED for entity scanning!");
                log.info("");
                log.info("    Total packages: {}", packages.size());

                // Separate consumer and starter packages
                long consumerCount = packages.stream()
                        .filter(pkg -> !pkg.startsWith("com.galoong.aiprompttracker"))
                        .count();
                long starterCount = packages.stream()
                        .filter(pkg -> pkg.startsWith("com.galoong.aiprompttracker"))
                        .count();

                // Show packages
                packages.forEach(pkg -> log.info("       - {}", pkg));

                log.info("");

                // Check if starter package is present
                if (starterCount == 0) {
                    log.error("    ❌ CRITICAL: Starter package 'com.galoong.aiprompttracker' NOT in EntityScanPackages!");
                    log.error("    → Starter entities will NOT be discovered!");
                    log.error("    → Hibernate ignores AutoConfigurationPackages when EntityScanPackages exists");
                    log.error("");
                    log.error("    SOLUTION: Add @EntityScan to your @SpringBootApplication:");
                    log.error("      @EntityScan(basePackages = {");
                    packages.forEach(pkg -> log.error("          \"{}\",", pkg));
                    log.error("          \"com.galoong.aiprompttracker.domain.entity\"");
                    log.error("      })");
                } else {
                    log.info("    ✅ Starter package included in EntityScanPackages");
                }
            }

        } catch (IllegalStateException e) {
            log.info("    ✅ EntityScanPackages NOT set (this is GOOD and expected)");
            log.info("    → Hibernate will use AutoConfigurationPackages for entity scanning");
            log.info("    → Both consumer and starter entities will be discovered from AutoConfigurationPackages");
        } catch (Exception e) {
            log.error("    ❌ Failed to read EntityScanPackages: {}", e.getMessage(), e);
        }

        log.info("");
    }

    private void diagnoseDiscoveredEntities(ConfigurableListableBeanFactory beanFactory) {
        log.info("[3] JPA Entities discovered by Hibernate:");

        try {
            EntityManagerFactory emf = beanFactory.getBean(EntityManagerFactory.class);
            Set<EntityType<?>> entities = emf.getMetamodel().getEntities();

            if (entities == null || entities.isEmpty()) {
                log.warn("    ⚠️  NO entities discovered by Hibernate!");
                log.warn("    → Check package registration and @Entity annotations");
                return;
            }

            log.info("    ✅ Total entities: {}", entities.size());
            log.info("");

            // Group by consumer vs starter
            long consumerCount = entities.stream()
                    .filter(e -> !e.getJavaType().getPackageName().startsWith("com.galoong.aiprompttracker"))
                    .count();
            long starterCount = entities.stream()
                    .filter(e -> e.getJavaType().getPackageName().startsWith("com.galoong.aiprompttracker"))
                    .count();

            log.info("    ✅ Consumer entities: {}", consumerCount);
            entities.stream()
                    .filter(e -> !e.getJavaType().getPackageName().startsWith("com.galoong.aiprompttracker"))
                    .forEach(e -> log.info("       - {}", e.getJavaType().getName()));

            log.info("");
            log.info("    ✅ Starter entities: {}", starterCount);
            entities.stream()
                    .filter(e -> e.getJavaType().getPackageName().startsWith("com.galoong.aiprompttracker"))
                    .forEach(e -> log.info("       - {}", e.getJavaType().getName()));

            // Warnings
            if (consumerCount == 0) {
                log.warn("");
                log.warn("    ⚠️  NO consumer entities found!");
                log.warn("    → If your app has entities, this indicates a scanning issue");
            }

            if (starterCount == 0) {
                log.warn("");
                log.warn("    ⚠️  NO starter entities found!");
                log.warn("    → This indicates package registration failed");
                log.warn("    → Check AiPromptTrackerJpaScanAutoConfiguration BFPP logs");
            }

        } catch (Exception e) {
            log.error("    ❌ Failed to read discovered entities: {}", e.getMessage(), e);
        }

        log.info("");
    }

    private void diagnoseRelevantProperties(Environment environment) {
        log.info("[4] Relevant Spring Boot / Hibernate Properties:");

        logProperty(environment, "spring.jpa.show-sql");
        logProperty(environment, "spring.jpa.hibernate.ddl-auto");
        logProperty(environment, "spring.jpa.properties.hibernate.dialect");
        logProperty(environment, "spring.jpa.properties.hibernate.format_sql");
        logProperty(environment, "ai-prompts.tracking.persistence.mode");
        logProperty(environment, "ai-prompts.debug.scan");

        log.info("");
    }

    private void logProperty(Environment environment, String key) {
        String value = environment.getProperty(key);
        if (value != null) {
            log.info("    {} = {}", key, value);
        } else {
            log.debug("    {} = <not set>", key);
        }
    }
}
