package com.galoong.aiprompttracker.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.domain.EntityScanPackages;
import org.springframework.core.Ordered;

import java.util.Arrays;
import java.util.List;

/**
 * Late-phase EntityScan detection mechanism via {@link BeanFactoryPostProcessor}.
 *
 * <h2>Why This Class Exists</h2>
 *
 * <p>The AI Prompt Tracker uses a dual-phase detection strategy for explicit {@code @EntityScan}:
 *
 * <h3>Phase 1: Early Detection (ImportBeanDefinitionRegistrar)</h3>
 * <ul>
 *   <li><b>When:</b> During {@code @Configuration} class processing (very early)</li>
 *   <li><b>Class:</b> {@link AiPromptTrackerJpaEntityScanWarningRegistrar}</li>
 *   <li><b>Method:</b> Checks {@code BeanDefinitionRegistry} for "entityScanPackages" bean definition</li>
 *   <li><b>Problem:</b> EntityScanPackages bean definition may not exist yet at this early stage</li>
 *   <li><b>Result:</b> Detection is opportunistic - may succeed or may miss</li>
 * </ul>
 *
 * <h3>Phase 2: Late Detection (BeanFactoryPostProcessor) - THIS CLASS</h3>
 * <ul>
 *   <li><b>When:</b> After all bean definitions are loaded (late)</li>
 *   <li><b>Order:</b> {@link Ordered#LOWEST_PRECEDENCE} - runs last</li>
 *   <li><b>Method:</b> Queries {@code BeanFactory} for {@code EntityScanPackages} bean instance</li>
 *   <li><b>Guarantee:</b> If consumer used {@code @EntityScan}, EntityScanPackages WILL exist at this point</li>
 *   <li><b>Result:</b> Detection is guaranteed - never misses</li>
 * </ul>
 *
 * <h2>Spring Boot Startup Order</h2>
 *
 * <p>Understanding the startup order is critical to understanding why we need both phases:
 *
 * <pre>
 * 1. @Configuration class scanning begins
 * 2. ImportBeanDefinitionRegistrars execute (EARLY PHASE)
 *    ├─ AiPromptTrackerAutoConfigPackageRegistrar runs
 *    └─ AiPromptTrackerJpaEntityScanWarningRegistrar runs (may detect EntityScanPackages)
 * 3. @EnableJpaRepositories processing
 * 4. All bean definitions loaded
 * 5. BeanFactoryPostProcessors execute (LATE PHASE - THIS CLASS)
 *    └─ EntityScanWarningBeanFactoryPostProcessor runs (guaranteed to detect)
 * 6. Bean instantiation begins
 * 7. Application ready
 * </pre>
 *
 * <h2>Why ImportBeanDefinitionRegistrar Can Miss EntityScanPackages</h2>
 *
 * <p>The {@code @EntityScan} annotation is processed by {@code EntityScanPackages.Registrar}
 * (also an ImportBeanDefinitionRegistrar). The order of execution between different registrars
 * is not guaranteed unless explicitly controlled.
 *
 * <p>Depending on configuration class scanning order:
 * <ul>
 *   <li><b>Scenario A:</b> Consumer's {@code @EntityScan} processed first
 *       → EntityScanPackages bean definition exists
 *       → Early detection succeeds ✅</li>
 *   <li><b>Scenario B:</b> Starter's registrar runs first
 *       → EntityScanPackages bean definition doesn't exist yet
 *       → Early detection fails ❌
 *       → Late detection catches it ✅</li>
 * </ul>
 *
 * <h2>Why BeanFactoryPostProcessor Is Reliable</h2>
 *
 * <p>By the time BeanFactoryPostProcessors run:
 * <ul>
 *   <li>ALL configuration classes have been processed</li>
 *   <li>ALL ImportBeanDefinitionRegistrars have executed</li>
 *   <li>ALL bean definitions are fully registered</li>
 *   <li>EntityScanPackages bean DEFINITELY exists if {@code @EntityScan} was used</li>
 * </ul>
 *
 * <h2>Duplicate Warning Prevention</h2>
 *
 * <p>Both detection phases use {@link EntityScanWarningState} to ensure only ONE warning is emitted:
 * <ul>
 *   <li>First phase to detect the issue wins (emits warning)</li>
 *   <li>Second phase sees warning already emitted (skips silently)</li>
 *   <li>Thread-safe via {@link java.util.concurrent.atomic.AtomicBoolean}</li>
 * </ul>
 *
 * <h2>What This Class Does</h2>
 *
 * <ol>
 *   <li>Attempts to retrieve {@code EntityScanPackages} bean from BeanFactory</li>
 *   <li>If NOT found: Consumer didn't use {@code @EntityScan} → return silently</li>
 *   <li>If found: Extract package list via {@code getPackageNames()}</li>
 *   <li>Check if starter entity package is included</li>
 *   <li>If missing: Emit warning via {@link EntityScanWarningState} (if not already emitted)</li>
 * </ol>
 *
 * <h2>What This Class Does NOT Do</h2>
 *
 * <ul>
 *   <li>❌ Does NOT create EntityScanPackages</li>
 *   <li>❌ Does NOT modify EntityScanPackages</li>
 *   <li>❌ Does NOT register any packages</li>
 *   <li>❌ Does NOT fix the issue automatically</li>
 *   <li>✅ ONLY detects and warns</li>
 * </ul>
 *
 * @see AiPromptTrackerJpaEntityScanWarningRegistrar
 * @see EntityScanWarningState
 * @see EntityScanPackages
 */
@Slf4j
public class EntityScanWarningBeanFactoryPostProcessor implements BeanFactoryPostProcessor, Ordered {

    private static final String STARTER_ENTITY_PACKAGE = "com.galoong.aiprompttracker.domain.entity";

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        log.debug("AI Prompt Tracker: Late-phase EntityScan detection started (BeanFactoryPostProcessor)");

        try {
            // Attempt to retrieve EntityScanPackages bean
            EntityScanPackages entityScanPackages = getEntityScanPackagesIfExists(beanFactory);

            if (entityScanPackages == null) {
                // Consumer did NOT use @EntityScan - this is the default and preferred case
                log.debug("AI Prompt Tracker: No EntityScanPackages found (consumer uses default scanning) - OK");
                return;
            }

            log.debug("AI Prompt Tracker: EntityScanPackages detected - checking if starter package is included...");

            // Extract package list
            List<String> packages = Arrays.asList(entityScanPackages.getPackageNames().toArray(new String[0]));

            if (packages.isEmpty()) {
                log.debug("AI Prompt Tracker: EntityScanPackages is empty (unusual but not critical)");
                return;
            }

            // Check if starter package is included
            boolean hasStarterPackage = EntityScanWarningState.hasStarterPackage(packages);

            if (hasStarterPackage) {
                log.info("AI Prompt Tracker: ✅ Starter entity package is included in @EntityScan - good!");
                return;
            }

            // Starter package is MISSING - emit warning (if not already emitted)
            log.debug("AI Prompt Tracker: Starter entity package MISSING from @EntityScan - attempting to emit warning");

            boolean emitted = EntityScanWarningState.tryEmitWarning(packages, "YourApplication");

            if (emitted) {
                log.debug("AI Prompt Tracker: Warning emitted successfully in late phase");
            } else {
                log.debug("AI Prompt Tracker: Warning already emitted by early phase, skipping");
            }

        } catch (Exception e) {
            // Non-critical - log and continue
            log.debug("AI Prompt Tracker: Could not check EntityScanPackages in late phase: {}", e.getMessage());
        }
    }

    /**
     * Attempts to retrieve EntityScanPackages bean from the BeanFactory.
     *
     * @param beanFactory the bean factory
     * @return EntityScanPackages instance if it exists, null otherwise
     */
    private EntityScanPackages getEntityScanPackagesIfExists(ConfigurableListableBeanFactory beanFactory) {
        try {
            // Try to get the bean - will throw NoSuchBeanDefinitionException if not found
            return beanFactory.getBean(EntityScanPackages.class);
        } catch (Exception e) {
            // Bean doesn't exist - consumer didn't use @EntityScan
            log.debug("AI Prompt Tracker: EntityScanPackages bean not found: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Returns lowest precedence to run LAST among all BeanFactoryPostProcessors.
     * This ensures all other registrations are complete before we check.
     *
     * @return {@link Ordered#LOWEST_PRECEDENCE}
     */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
