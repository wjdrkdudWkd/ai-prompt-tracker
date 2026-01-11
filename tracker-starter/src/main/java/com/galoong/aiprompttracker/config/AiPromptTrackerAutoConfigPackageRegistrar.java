package com.galoong.aiprompttracker.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Safely appends the AI Prompt Tracker base package to Spring Boot's AutoConfigurationPackages.
 *
 * <p><b>CRITICAL - Append-Only Safety:</b>
 * This registrar uses {@link BeanFactoryPostProcessor} to ensure it ONLY appends the starter's
 * package when AutoConfigurationPackages already exists. It NEVER creates or owns the
 * AutoConfigurationPackages bean, preventing interference with consumer entity scanning.
 *
 * <p><b>How It Works:</b>
 * <ol>
 *   <li>Waits until BeanFactoryPostProcessor phase (after initial bean definitions loaded)</li>
 *   <li>Checks if AutoConfigurationPackages bean already exists (consumer's package registered)</li>
 *   <li>If exists: Appends {@code com.galoong.aiprompttracker} to existing packages</li>
 *   <li>If not exists: Skips registration to avoid breaking consumer scanning</li>
 *   <li>Result: Consumer packages are never affected; starter package added only when safe</li>
 * </ol>
 *
 * <p><b>Why This Approach is Safe:</b>
 * <ul>
 *   <li><b>Never creates AutoConfigurationPackages</b> - Only appends to existing bean</li>
 *   <li><b>Ordering-independent</b> - Works regardless of auto-configuration order</li>
 *   <li><b>Consumer entities always work</b> - Their base package is registered first by Boot</li>
 *   <li><b>Graceful degradation</b> - If AutoConfigurationPackages not available, logs and skips</li>
 * </ul>
 *
 * <p><b>Benefits:</b>
 * <ul>
 *   <li>Consumer repositories/entities continue to work (guaranteed no interference)</li>
 *   <li>Starter repositories are auto-discovered (when AutoConfigurationPackages available)</li>
 *   <li>True plug-and-play experience</li>
 *   <li>Follows Spring Boot auto-configuration best practices</li>
 * </ul>
 *
 * <p><b>Why Not {@code @EnableJpaRepositories}?</b>
 * Using {@code @EnableJpaRepositories} in a starter causes Spring Boot's
 * {@code JpaRepositoriesAutoConfiguration} to completely back off, breaking consumer repository
 * scanning. The AutoConfigurationPackages append approach extends (rather than replaces) Boot's
 * default scanning.
 *
 * <p><b>Verification:</b>
 * To verify this is working in a consumer application:
 * <ol>
 *   <li>Run with debug logging and check for log message: "AI Prompt Tracker: appended '...' to AutoConfigurationPackages"</li>
 *   <li>Verify both consumer repositories (e.g., WordEntryRepository) and starter repositories (ExecutionRepository, CallRepository) are created</li>
 *   <li>Verify no "Not a managed type" errors occur for any entities</li>
 *   <li>Test starter API endpoints (e.g., /aiprompt-tracker/api/dashboard/summary) respond correctly</li>
 * </ol>
 *
 * @see AutoConfigurationPackages
 * @see org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration
 * @see BeanFactoryPostProcessor
 */
@Slf4j
@Component
public class AiPromptTrackerAutoConfigPackageRegistrar implements BeanFactoryPostProcessor {

    private static final String STARTER_BASE_PACKAGE = "com.galoong.aiprompttracker";

    /**
     * Safely appends the starter's base package to AutoConfigurationPackages if available.
     *
     * <p>This method performs an append-only operation that never creates or replaces
     * the AutoConfigurationPackages bean, ensuring consumer entity scanning is never broken.
     *
     * @param beanFactory the bean factory used by the application context
     * @throws BeansException in case of errors
     */
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // CRITICAL SAFETY CHECK 1: Only proceed if AutoConfigurationPackages already exists
        // This ensures consumer's base package is registered first by Spring Boot
        if (!AutoConfigurationPackages.has(beanFactory)) {
            log.debug("AI Prompt Tracker: AutoConfigurationPackages not yet available; " +
                    "skipping package append to avoid breaking consumer scanning");
            return;
        }

        // CRITICAL SAFETY CHECK 2: Ensure BeanFactory is a BeanDefinitionRegistry
        if (!(beanFactory instanceof org.springframework.beans.factory.support.BeanDefinitionRegistry)) {
            log.debug("AI Prompt Tracker: BeanFactory is not a BeanDefinitionRegistry; " +
                    "cannot append package to AutoConfigurationPackages");
            return;
        }

        org.springframework.beans.factory.support.BeanDefinitionRegistry registry =
                (org.springframework.beans.factory.support.BeanDefinitionRegistry) beanFactory;

        // Wrap entire operation in try-catch to ensure consumer startup never fails
        try {
            // CRITICAL SAFETY CHECK 3: Check if our package is already registered
            List<String> existingPackages = AutoConfigurationPackages.get(beanFactory);
            if (existingPackages.contains(STARTER_BASE_PACKAGE)) {
                log.debug("AI Prompt Tracker: package '{}' already registered in AutoConfigurationPackages",
                        STARTER_BASE_PACKAGE);
                return;
            }

            // APPEND-ONLY OPERATION: Register ONLY our package (does not replace existing)
            // AutoConfigurationPackages.register() performs an additive registration when called
            // with a single package - it does NOT replace the entire list
            AutoConfigurationPackages.register(registry, STARTER_BASE_PACKAGE);

            log.debug("AI Prompt Tracker: appended '{}' to AutoConfigurationPackages", STARTER_BASE_PACKAGE);

        } catch (Exception e) {
            // CRITICAL: Never fail consumer startup due to starter package registration
            // Log warning and continue - consumer scanning must not be affected
            log.warn("AI Prompt Tracker: Failed to append package to AutoConfigurationPackages. " +
                    "Starter repositories may not be auto-discovered. Error: {}", e.getMessage());
        }
    }
}
