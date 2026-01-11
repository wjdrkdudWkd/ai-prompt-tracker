package com.galoong.aiprompttracker.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScanPackages;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Auto-configuration for JPA entity and repository scanning.
 *
 * <p><b>CRITICAL - AutoConfigurationPackages-Only Strategy:</b>
 * This configuration registers a {@link BeanFactoryPostProcessor} as a <b>static @Bean</b> that
 * safely appends the starter's base package to {@link AutoConfigurationPackages} ONLY.
 *
 * <p><b>Why ONLY AutoConfigurationPackages?</b>
 * <ul>
 *   <li><b>Repository Scanning</b>: Spring Data JPA uses AutoConfigurationPackages to discover @Repository interfaces</li>
 *   <li><b>Entity Scanning</b>: Hibernate ALSO uses AutoConfigurationPackages as fallback when EntityScanPackages is NOT set</li>
 *   <li><b>CRITICAL SAFETY</b>: We NEVER create EntityScanPackages ourselves. Creating it can break consumer entity scanning!</li>
 *   <li><b>Consumer entities work</b>: Their base package is in AutoConfigurationPackages (registered by @SpringBootApplication)</li>
 *   <li><b>Starter entities work</b>: Our package is appended to AutoConfigurationPackages</li>
 * </ul>
 *
 * <p><b>Why NOT EntityScanPackages?</b>
 * <ul>
 *   <li>If EntityScanPackages EXISTS, Hibernate scans ONLY those packages (ignoring AutoConfigurationPackages)</li>
 *   <li>If we CREATE EntityScanPackages with only our package, consumer entities become "Not a managed type"</li>
 *   <li>If we try to APPEND to EntityScanPackages, we might create it when it doesn't exist</li>
 *   <li><b>Solution</b>: Never touch EntityScanPackages. Let Hibernate use AutoConfigurationPackages fallback.</li>
 * </ul>
 *
 * <p><b>Safety Guarantees:</b>
 * <ul>
 *   <li><b>Never creates AutoConfigurationPackages</b> - Only appends when it already exists</li>
 *   <li><b>Never replaces consumer packages</b> - Pure append-only operation</li>
 *   <li><b>Never creates EntityScanPackages</b> - Avoids breaking consumer entity scanning</li>
 *   <li><b>Ordering-safe</b> - Runs after JpaRepositoriesAutoConfiguration, before HibernateJpaAutoConfiguration</li>
 *   <li><b>Graceful degradation</b> - Skips if AutoConfigurationPackages not available</li>
 *   <li><b>Never fails consumer startup</b> - All operations wrapped in try-catch</li>
 * </ul>
 *
 * <p><b>Load Order:</b>
 * <ul>
 *   <li>{@code @AutoConfigureAfter(JpaRepositoriesAutoConfiguration.class)} - Run after repository auto-config</li>
 *   <li>{@code @AutoConfigureBefore(HibernateJpaAutoConfiguration.class)} - Run before entity manager creation</li>
 * </ul>
 *
 * @see AutoConfigurationPackages
 * @see JpaRepositoriesAutoConfiguration
 * @see HibernateJpaAutoConfiguration
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(JpaRepositoriesAutoConfiguration.class)
@AutoConfigureBefore(HibernateJpaAutoConfiguration.class)
public class AiPromptTrackerJpaScanAutoConfiguration {

    private static final String STARTER_BASE_PACKAGE = "com.galoong.aiprompttracker";

    /**
     * Static bean factory post processor for early package registration.
     *
     * <p><b>Why Static?</b>
     * Static @Bean methods are invoked before the containing @Configuration class is instantiated,
     * ensuring the BFPP runs very early in the Spring lifecycle - before EntityManagerFactory
     * creation and entity scanning.
     *
     * <p><b>What It Does:</b>
     * Appends starter package to AutoConfigurationPackages ONLY. Does NOT touch EntityScanPackages.
     *
     * @return BFPP that appends starter package to AutoConfigurationPackages
     */
    @Bean
    public static BeanFactoryPostProcessor aiPromptTrackerPackageRegistrar() {
        return new SafePackageAppendingBeanFactoryPostProcessor();
    }

    /**
     * BeanFactoryPostProcessor that safely appends starter package to AutoConfigurationPackages.
     *
     * <p><b>CRITICAL SAFETY:</b>
     * This BFPP performs an append-only operation on AutoConfigurationPackages and NEVER touches
     * EntityScanPackages. This ensures consumer entity/repository scanning is never broken.
     *
     * <p><b>How Scanning Works:</b>
     * <ul>
     *   <li>Spring Data JPA scans AutoConfigurationPackages for repositories</li>
     *   <li>Hibernate scans EntityScanPackages if it exists, otherwise falls back to AutoConfigurationPackages</li>
     *   <li>By only appending to AutoConfigurationPackages, both consumer and starter packages are scanned</li>
     * </ul>
     */
    @Slf4j
    private static class SafePackageAppendingBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            log.info("AI Prompt Tracker: JPA scan BFPP started - appending package to AutoConfigurationPackages");

            // CRITICAL SAFETY CHECK: Ensure BeanFactory is a BeanDefinitionRegistry
            if (!(beanFactory instanceof BeanDefinitionRegistry)) {
                log.warn("AI Prompt Tracker: BeanFactory is not a BeanDefinitionRegistry; " +
                        "cannot append package. This should not happen in normal Spring Boot apps.");
                return;
            }

            BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

            // Append to AutoConfigurationPackages ONLY (never EntityScanPackages)
            appendToAutoConfigurationPackages(beanFactory, registry);

            // Check if EntityScanPackages exists and warn if problematic
            checkEntityScanPackages(beanFactory);

            log.info("AI Prompt Tracker: JPA scan BFPP completed successfully");
        }

        /**
         * Safely append to AutoConfigurationPackages (for BOTH repository and entity scanning).
         *
         * <p><b>CRITICAL:</b> This is the ONLY registry we modify. We never touch EntityScanPackages.
         */
        private void appendToAutoConfigurationPackages(
                ConfigurableListableBeanFactory beanFactory,
                BeanDefinitionRegistry registry) {

            try {
                // CRITICAL SAFETY CHECK 1: Only proceed if AutoConfigurationPackages already exists
                if (!AutoConfigurationPackages.has(beanFactory)) {
                    log.warn("AI Prompt Tracker: AutoConfigurationPackages not yet available; " +
                            "skipping package append. This should not happen in normal Spring Boot apps.");
                    log.warn("AI Prompt Tracker: Starter repositories and entities will NOT be discovered!");
                    return;
                }

                // Get existing packages for logging and validation
                List<String> existingPackages = AutoConfigurationPackages.get(beanFactory);
                log.debug("AI Prompt Tracker: Existing AutoConfigurationPackages: {}", existingPackages);

                // CRITICAL VALIDATION: Check if consumer package is present
                boolean hasConsumerPackage = existingPackages.stream()
                        .anyMatch(pkg -> !pkg.startsWith("com.galoong.aiprompttracker"));

                if (!hasConsumerPackage && existingPackages.size() == 1 &&
                    existingPackages.contains(STARTER_BASE_PACKAGE)) {
                    log.warn("╔═══════════════════════════════════════════════════════════════════════════════╗");
                    log.warn("║  ⚠️  WARNING: Consumer base package NOT found in AutoConfigurationPackages!  ║");
                    log.warn("╚═══════════════════════════════════════════════════════════════════════════════╝");
                    log.warn("Current packages: {}", existingPackages);
                    log.warn("This usually means:");
                    log.warn("  1) Consumer's @SpringBootApplication is in the same package as the starter");
                    log.warn("  2) AutoConfigurationPackages was registered incorrectly");
                    log.warn("If this is a real consumer app (not demo), consumer entities may fail with 'Not a managed type'");
                }

                // CRITICAL SAFETY CHECK 2: Check if our package is already registered
                if (existingPackages.contains(STARTER_BASE_PACKAGE)) {
                    log.debug("AI Prompt Tracker: package '{}' already in AutoConfigurationPackages",
                            STARTER_BASE_PACKAGE);
                    return;
                }

                // APPEND-ONLY OPERATION: Register ONLY our package
                // AutoConfigurationPackages.register() performs ADDITIVE registration
                // It does NOT replace existing packages
                AutoConfigurationPackages.register(registry, STARTER_BASE_PACKAGE);

                log.info("AI Prompt Tracker: ✅ Appended '{}' to AutoConfigurationPackages", STARTER_BASE_PACKAGE);

                // Verify registration succeeded
                List<String> updatedPackages = AutoConfigurationPackages.get(beanFactory);
                log.info("AI Prompt Tracker: Updated AutoConfigurationPackages: {}", updatedPackages);

                // Final validation
                if (!updatedPackages.contains(STARTER_BASE_PACKAGE)) {
                    log.error("AI Prompt Tracker: ❌ Package append FAILED! Starter package not in updated list.");
                    log.error("This is a BUG. Please report at: https://github.com/wjdrkdudWkd/ai-prompt-tracker/issues");
                }

            } catch (Exception e) {
                // CRITICAL: Never fail consumer startup
                log.error("AI Prompt Tracker: Failed to append to AutoConfigurationPackages. " +
                        "Starter repositories and entities will NOT be auto-discovered.", e);
                log.error("Error: {}", e.getMessage());
            }
        }

        /**
         * Check if EntityScanPackages exists and warn if it might cause issues.
         *
         * <p><b>Why We Check:</b>
         * If EntityScanPackages exists but doesn't include our package, Hibernate will ONLY scan
         * those packages and ignore AutoConfigurationPackages (including our package).
         *
         * <p><b>What We Do:</b>
         * We log a warning but DON'T modify EntityScanPackages. The user must fix their configuration.
         */
        private void checkEntityScanPackages(ConfigurableListableBeanFactory beanFactory) {
            try {
                EntityScanPackages entityScanPackages = EntityScanPackages.get(beanFactory);
                List<String> packages = entityScanPackages.getPackageNames();

                log.info("AI Prompt Tracker: EntityScanPackages exists: {}", packages);

                // Check if our package is included
                if (!packages.contains(STARTER_BASE_PACKAGE)) {
                    log.warn("╔═══════════════════════════════════════════════════════════════════════════════╗");
                    log.warn("║  ⚠️  WARNING: EntityScanPackages exists but does NOT include starter package! ║");
                    log.warn("╚═══════════════════════════════════════════════════════════════════════════════╝");
                    log.warn("EntityScanPackages: {}", packages);
                    log.warn("Hibernate will ONLY scan these packages, ignoring AutoConfigurationPackages!");
                    log.warn("Starter entities will NOT be discovered!");
                    log.warn("");
                    log.warn("SOLUTION: Add @EntityScan to your @SpringBootApplication:");
                    log.warn("  @EntityScan(basePackages = {{");
                    packages.forEach(pkg -> log.warn("      \"{}\",", pkg));
                    log.warn("      \"com.galoong.aiprompttracker.domain.entity\"");
                    log.warn("  }})");
                }

            } catch (IllegalStateException e) {
                // EntityScanPackages not registered - this is GOOD and EXPECTED
                log.debug("AI Prompt Tracker: EntityScanPackages not set (this is GOOD)");
                log.debug("Hibernate will use AutoConfigurationPackages for entity scanning");
            } catch (Exception e) {
                log.debug("AI Prompt Tracker: Could not check EntityScanPackages: {}", e.getMessage());
            }
        }
    }
}
