package com.galoong.aiprompttracker.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

/**
 * Registers the starter's base package into {@link AutoConfigurationPackages} for JPA scanning.
 *
 * <p><b>CRITICAL - Early Registration:</b>
 * This class implements {@link ImportBeanDefinitionRegistrar} which runs EARLY during configuration
 * class processing, BEFORE Spring Data JPA repository scanning begins. This is the key difference
 * from BeanFactoryPostProcessor which runs too late.
 *
 * <p><b>How It Works:</b>
 * <ul>
 *   <li>Runs during @Configuration class processing (very early)</li>
 *   <li>Appends starter package to AutoConfigurationPackages</li>
 *   <li>Repository scanning sees the updated AutoConfigurationPackages</li>
 *   <li>Both consumer and starter repositories are discovered</li>
 * </ul>
 *
 * <p><b>Why AutoConfigurationPackages?</b>
 * <ul>
 *   <li>Spring Data JPA scans AutoConfigurationPackages for @Repository interfaces</li>
 *   <li>Hibernate also uses AutoConfigurationPackages for @Entity classes (when EntityScanPackages not set)</li>
 *   <li>Consumer's @SpringBootApplication already registered consumer package</li>
 *   <li>We append our package to discover starter entities/repositories</li>
 * </ul>
 *
 * <p><b>Safety:</b>
 * <ul>
 *   <li>Only appends - never replaces consumer packages</li>
 *   <li>Never creates EntityScanPackages (avoids breaking consumer entity scanning)</li>
 *   <li>Runs early enough to affect repository scanning</li>
 * </ul>
 *
 * @see AutoConfigurationPackages
 * @see org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration
 */
@Slf4j
public class AiPromptTrackerAutoConfigPackageRegistrar implements ImportBeanDefinitionRegistrar {

    private static final String STARTER_BASE_PACKAGE = "com.galoong.aiprompttracker";

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        log.debug("AI Prompt Tracker: Registrar started - registering base package to AutoConfigurationPackages");

        try {
            // Register starter base package to AutoConfigurationPackages
            // This is ADDITIVE - it appends to existing packages, does not replace them
            AutoConfigurationPackages.register(registry, STARTER_BASE_PACKAGE);

            log.info("AI Prompt Tracker: ✅ Registered base package '{}' to AutoConfigurationPackages",
                    STARTER_BASE_PACKAGE);
            log.debug("AI Prompt Tracker: Repository and entity scanning will include starter package");

        } catch (Exception e) {
            log.error("AI Prompt Tracker: ❌ Failed to register base package to AutoConfigurationPackages", e);
            log.error("Starter repositories and entities will NOT be auto-discovered!");
            log.error("Error: {}", e.getMessage());
        }
    }
}
