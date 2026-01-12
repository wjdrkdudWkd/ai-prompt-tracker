package com.galoong.aiprompttracker.autoconfigure;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Shared state for EntityScan warning to prevent duplicate warnings.
 *
 * <p>The AI Prompt Tracker uses a dual-phase detection strategy for explicit @EntityScan:
 * <ol>
 *   <li><b>Early Phase</b> - {@link AiPromptTrackerJpaEntityScanWarningRegistrar} (ImportBeanDefinitionRegistrar)
 *       <br>Runs during @Configuration processing, may detect EntityScanPackages if already registered</li>
 *   <li><b>Late Phase</b> - {@link EntityScanWarningBeanFactoryPostProcessor} (BeanFactoryPostProcessor)
 *       <br>Runs after all bean definitions loaded, guaranteed to detect EntityScanPackages if present</li>
 * </ol>
 *
 * <p>This shared state ensures that only ONE warning is emitted, regardless of which phase detects the issue.
 *
 * <p><b>Thread Safety:</b> Uses AtomicBoolean for safe concurrent access across multiple contexts and tests.
 *
 * @see AiPromptTrackerJpaEntityScanWarningRegistrar
 * @see EntityScanWarningBeanFactoryPostProcessor
 */
@Slf4j
public final class EntityScanWarningState {

    private static final AtomicBoolean WARNING_EMITTED = new AtomicBoolean(false);

    private static final String STARTER_ENTITY_PACKAGE = "com.galoong.aiprompttracker.domain.entity";
    private static final String STARTER_BASE_PACKAGE = "com.galoong.aiprompttracker";

    /**
     * Private constructor - utility class.
     */
    private EntityScanWarningState() {
    }

    /**
     * Attempts to emit the EntityScan warning if not already emitted.
     *
     * @param existingPackages the packages specified in consumer's @EntityScan
     * @param consumerAppClassName inferred consumer application class name (or "YourApplication")
     * @return true if warning was emitted, false if already emitted by another phase
     */
    public static boolean tryEmitWarning(List<String> existingPackages, String consumerAppClassName) {
        // Atomic check-and-set: only one thread/phase wins
        if (!WARNING_EMITTED.compareAndSet(false, true)) {
            log.debug("AI Prompt Tracker: EntityScan warning already emitted by another detection phase, skipping");
            return false;
        }

        logMissingPackageWarning(existingPackages, consumerAppClassName);
        return true;
    }

    /**
     * Checks if the starter entity package is included in the given package list.
     *
     * @param packages list of packages from @EntityScan
     * @return true if starter package is included
     */
    public static boolean hasStarterPackage(List<String> packages) {
        if (packages == null || packages.isEmpty()) {
            return false;
        }

        return packages.stream()
                .anyMatch(pkg -> pkg.equals(STARTER_ENTITY_PACKAGE) ||
                        pkg.equals(STARTER_BASE_PACKAGE) ||
                        pkg.startsWith(STARTER_BASE_PACKAGE + "."));
    }

    /**
     * Resets the warning state (for testing only).
     * DO NOT call in production code.
     */
    public static void resetForTesting() {
        WARNING_EMITTED.set(false);
    }

    /**
     * Logs a detailed warning when starter entity package is missing from @EntityScan.
     *
     * @param existingPackages the packages consumer specified in @EntityScan
     * @param consumerAppClassName consumer application class name (for snippet generation)
     */
    private static void logMissingPackageWarning(List<String> existingPackages, String consumerAppClassName) {
        log.warn("");
        log.warn("╔═══════════════════════════════════════════════════════════════════════════════╗");
        log.warn("║                              ⚠️  CONFIGURATION WARNING ⚠️                      ║");
        log.warn("║              Starter Entities Will NOT Be Discovered by Hibernate             ║");
        log.warn("╚═══════════════════════════════════════════════════════════════════════════════╝");
        log.warn("");
        log.warn("Your application uses @EntityScan which tells Hibernate to scan ONLY specific packages.");
        log.warn("This overrides Spring Boot's default scanning (AutoConfigurationPackages).");
        log.warn("");
        log.warn("Current @EntityScan packages:");
        existingPackages.forEach(pkg -> log.warn("  • {}", pkg));
        log.warn("");
        log.warn("Missing: {}", STARTER_ENTITY_PACKAGE);
        log.warn("");
        log.warn("═══════════════════════════════════════════════════════════════════════════════");
        log.warn("SOLUTION: Add starter entity package to your @EntityScan:");
        log.warn("═══════════════════════════════════════════════════════════════════════════════");
        log.warn("");

        log.warn("@SpringBootApplication");
        log.warn("@EntityScan(basePackages = {");

        // Show existing packages
        existingPackages.forEach(pkg -> log.warn("    \"{}\",", pkg));

        // Add starter package
        log.warn("    \"{}\"  // Add this line!", STARTER_ENTITY_PACKAGE);

        log.warn("})");
        log.warn("public class {} {{", consumerAppClassName);
        log.warn("    public static void main(String[] args) {");
        log.warn("        SpringApplication.run({}.class, args);", consumerAppClassName);
        log.warn("    }");
        log.warn("}");
        log.warn("");
        log.warn("═══════════════════════════════════════════════════════════════════════════════");
        log.warn("IMPACT:");
        log.warn("  • Starter entities (Execution, Call) will NOT be managed by JPA");
        log.warn("  • Starter repositories (ExecutionRepository, CallRepository) may fail");
        log.warn("  • Application may fail at runtime if persistence mode requires starter entities");
        log.warn("");
        log.warn("To verify this is correctly configured, enable diagnostic mode:");
        log.warn("  ai-prompts.debug.scan=true");
        log.warn("═══════════════════════════════════════════════════════════════════════════════");
        log.warn("");
    }
}
