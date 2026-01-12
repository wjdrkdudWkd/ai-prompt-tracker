package com.galoong.aiprompttracker.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.domain.EntityScanPackages;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Detects when consumer uses explicit {@code @EntityScan} and warns if starter entity package is missing.
 *
 * <p><b>Problem We're Solving:</b>
 * When a consumer application uses {@code @EntityScan}, Spring Boot creates an {@link EntityScanPackages}
 * bean. Hibernate then scans ONLY those packages and ignores {@link org.springframework.boot.autoconfigure.AutoConfigurationPackages}.
 * This breaks starter entity discovery unless the consumer explicitly includes our package.
 *
 * <p><b>Why We Can't Auto-Fix:</b>
 * <ul>
 *   <li>Creating/modifying EntityScanPackages from a starter is dangerous and error-prone</li>
 *   <li>We might override consumer's explicit configuration</li>
 *   <li>EntityScanPackages might already be registered by the time we run</li>
 *   <li><b>Solution:</b> Detect the issue and provide clear actionable warning</li>
 * </ul>
 *
 * <p><b>When This Runs:</b>
 * This registrar implements {@link ImportBeanDefinitionRegistrar} which runs EARLY during
 * {@code @Configuration} class processing, BEFORE Hibernate entity scanning. This is the right
 * time to check EntityScanPackages and warn the consumer.
 *
 * <p><b>Detection Strategy:</b>
 * <ol>
 *   <li>Check if EntityScanPackages bean definition exists in registry</li>
 *   <li>If YES: Extract the list of packages consumer specified</li>
 *   <li>Check if our starter entity package is included</li>
 *   <li>If MISSING: Log detailed warning with exact copy-paste snippet</li>
 * </ol>
 *
 * <p><b>Warning Content:</b>
 * The warning includes:
 * <ul>
 *   <li>Explanation of why starter entities won't be discovered</li>
 *   <li>Exact {@code @EntityScan} snippet to copy-paste</li>
 *   <li>Consumer's existing packages (preserved)</li>
 *   <li>Starter entity package to add</li>
 * </ul>
 *
 * <p><b>Example Warning Output:</b>
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                              ⚠️  CONFIGURATION WARNING ⚠️                      ║
 * ║              Starter Entities Will NOT Be Discovered by Hibernate             ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * Your application uses @EntityScan which tells Hibernate to scan ONLY specific packages.
 * This overrides Spring Boot's default scanning (AutoConfigurationPackages).
 *
 * Current @EntityScan packages:
 *   • com.yourcompany.yourapp.domain
 *
 * Missing: com.galoong.aiprompttracker.domain.entity
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 * SOLUTION: Add starter entity package to your @EntityScan:
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * {@literal @}SpringBootApplication
 * {@literal @}EntityScan(basePackages = {
 *     "com.yourcompany.yourapp.domain",
 *     "com.galoong.aiprompttracker.domain.entity"  // Add this line!
 * })
 * public class YourApplication {
 *     public static void main(String[] args) {
 *         SpringApplication.run(YourApplication.class, args);
 *     }
 * }
 * </pre>
 *
 * @see EntityScanPackages
 * @see AiPromptTrackerAutoConfigPackageRegistrar
 * @see org.springframework.boot.autoconfigure.domain.EntityScan
 */
@Slf4j
public class AiPromptTrackerJpaEntityScanWarningRegistrar implements ImportBeanDefinitionRegistrar {

    private static final String STARTER_ENTITY_PACKAGE = "com.galoong.aiprompttracker.domain.entity";
    private static final String STARTER_BASE_PACKAGE = "com.galoong.aiprompttracker";
    private static final String ENTITY_SCAN_PACKAGES_BEAN_NAME = "entityScanPackages";

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        log.debug("AI Prompt Tracker: Checking for explicit @EntityScan configuration...");

        try {
            // Check if EntityScanPackages bean definition exists
            if (!registry.containsBeanDefinition(ENTITY_SCAN_PACKAGES_BEAN_NAME)) {
                log.debug("AI Prompt Tracker: No explicit @EntityScan detected (GOOD - using AutoConfigurationPackages)");
                return;
            }

            log.debug("AI Prompt Tracker: Explicit @EntityScan detected - checking if starter package is included...");

            // EntityScanPackages exists - extract the packages
            List<String> entityScanPackages = extractEntityScanPackages(registry);

            if (entityScanPackages == null || entityScanPackages.isEmpty()) {
                log.warn("AI Prompt Tracker: EntityScanPackages exists but is empty - this is unusual");
                return;
            }

            // Check if starter package is already included
            boolean hasStarterPackage = entityScanPackages.stream()
                    .anyMatch(pkg -> pkg.equals(STARTER_ENTITY_PACKAGE) ||
                            pkg.equals(STARTER_BASE_PACKAGE) ||
                            pkg.startsWith(STARTER_BASE_PACKAGE + "."));

            if (hasStarterPackage) {
                log.info("AI Prompt Tracker: ✅ Starter entity package is included in @EntityScan - good!");
                return;
            }

            // Starter package is MISSING - log detailed warning
            logMissingPackageWarning(entityScanPackages, importingClassMetadata);

        } catch (Exception e) {
            log.debug("AI Prompt Tracker: Could not check EntityScanPackages (this is OK): {}", e.getMessage());
        }
    }

    /**
     * Extracts the list of packages from EntityScanPackages bean definition.
     *
     * <p>This method attempts to read the packages using Spring Boot's internal structure.
     * The EntityScanPackages bean definition contains a constructor argument with the package list.
     */
    private List<String> extractEntityScanPackages(BeanDefinitionRegistry registry) {
        try {
            // Try to read via constructor args if available
            var beanDef = registry.getBeanDefinition(ENTITY_SCAN_PACKAGES_BEAN_NAME);
            var constructorArgs = beanDef.getConstructorArgumentValues();

            if (constructorArgs.isEmpty()) {
                return List.of();
            }

            // EntityScanPackages typically has a single constructor arg: String[] packages
            var firstArg = constructorArgs.getIndexedArgumentValue(0, Object.class);
            if (firstArg != null && firstArg.getValue() instanceof String[]) {
                return Arrays.asList((String[]) firstArg.getValue());
            }

            // Try generic argument
            var genericArg = constructorArgs.getGenericArgumentValues().stream().findFirst();
            if (genericArg.isPresent() && genericArg.get().getValue() instanceof String[]) {
                return Arrays.asList((String[]) genericArg.get().getValue());
            }

            return List.of();

        } catch (Exception e) {
            log.debug("AI Prompt Tracker: Could not extract EntityScanPackages list: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Logs a detailed warning when starter entity package is missing from @EntityScan.
     *
     * @param existingPackages the packages consumer specified in @EntityScan
     * @param importingClassMetadata metadata about the importing configuration class (for inferring consumer package)
     */
    private void logMissingPackageWarning(List<String> existingPackages, AnnotationMetadata importingClassMetadata) {
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

        // Infer consumer application class name if possible
        String consumerAppClassName = inferConsumerApplicationClassName(importingClassMetadata);

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

    /**
     * Attempts to infer the consumer application class name from metadata.
     *
     * @param metadata annotation metadata (may contain @SpringBootApplication class name)
     * @return inferred class name or "YourApplication" as fallback
     */
    private String inferConsumerApplicationClassName(AnnotationMetadata metadata) {
        if (metadata == null) {
            return "YourApplication";
        }

        try {
            // The importing class metadata might give us hints
            String className = metadata.getClassName();
            if (className != null && !className.contains("aiprompttracker")) {
                // Extract simple class name
                int lastDot = className.lastIndexOf('.');
                if (lastDot > 0 && lastDot < className.length() - 1) {
                    return className.substring(lastDot + 1);
                }
            }
        } catch (Exception e) {
            log.debug("Could not infer consumer application class name: {}", e.getMessage());
        }

        return "YourApplication";
    }
}
