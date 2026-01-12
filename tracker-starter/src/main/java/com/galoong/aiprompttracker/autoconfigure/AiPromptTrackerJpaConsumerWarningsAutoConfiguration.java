package com.galoong.aiprompttracker.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.domain.EntityScanPackages;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;

import jakarta.persistence.EntityManager;
import java.util.List;

/**
 * Auto-configuration that warns consumers when they use explicit @EntityScan without including
 * the starter's entity package.
 *
 * <p><b>Problem:</b>
 * When a consumer application uses {@code @EntityScan}, Spring creates an {@link EntityScanPackages}
 * bean. Hibernate then scans ONLY those packages and ignores AutoConfigurationPackages. This breaks
 * starter entity discovery.
 *
 * <p><b>Solution:</b>
 * This auto-configuration detects EntityScanPackages at runtime and logs a clear warning if the
 * starter entity package is missing. The warning includes an exact copy-paste snippet for the fix.
 *
 * <p><b>When Warning Appears:</b>
 * <ul>
 *   <li>JPA is on the classpath ({@code jakarta.persistence.EntityManager} present)</li>
 *   <li>Consumer has explicit {@code @EntityScan} (EntityScanPackages bean exists)</li>
 *   <li>Starter entity package NOT in EntityScanPackages</li>
 * </ul>
 *
 * <p><b>Example Fix:</b>
 * <pre>
 * {@code @SpringBootApplication}
 * {@code @EntityScan(basePackages = {}
 *     "com.yourcompany.yourapp.domain",
 *     "com.galoong.aiprompttracker.domain.entity"  // Add this!
 * })
 * public class YourApplication {}
 * </pre>
 *
 * @see EntityScanPackages
 * @see AiPromptTrackerJpaScanAutoConfiguration
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(EntityManager.class)
@AutoConfigureAfter(HibernateJpaAutoConfiguration.class)
public class AiPromptTrackerJpaConsumerWarningsAutoConfiguration {

    private static final String STARTER_ENTITY_PACKAGE = "com.galoong.aiprompttracker.domain.entity";

    @Bean
    public ApplicationListener<ApplicationReadyEvent> entityScanWarningListener(
            ConfigurableListableBeanFactory beanFactory) {

        return event -> checkEntityScanPackagesAndWarn(beanFactory);
    }

    private void checkEntityScanPackagesAndWarn(ConfigurableListableBeanFactory beanFactory) {
        try {
            // Attempt to get EntityScanPackages
            EntityScanPackages entityScanPackages = EntityScanPackages.get(beanFactory);
            List<String> packages = entityScanPackages.getPackageNames();

            if (packages == null || packages.isEmpty()) {
                // EntityScanPackages exists but is empty - unusual but not our concern
                return;
            }

            // Check if starter entity package is included
            boolean hasStarterPackage = packages.stream()
                    .anyMatch(pkg -> pkg.equals(STARTER_ENTITY_PACKAGE) ||
                            pkg.startsWith("com.galoong.aiprompttracker"));

            if (!hasStarterPackage) {
                // CRITICAL: Consumer uses @EntityScan but excluded starter entities
                logEntityScanWarning(packages);
            }

        } catch (IllegalStateException e) {
            // EntityScanPackages not registered - this is GOOD and expected
            // Consumer is NOT using explicit @EntityScan
            // No warning needed
            log.debug("AI Prompt Tracker: No explicit @EntityScan detected (good)");
        } catch (Exception e) {
            log.debug("AI Prompt Tracker: Could not check EntityScanPackages: {}", e.getMessage());
        }
    }

    private void logEntityScanWarning(List<String> existingPackages) {
        log.warn("");
        log.warn("╔═══════════════════════════════════════════════════════════════════════════════╗");
        log.warn("║                              ⚠️  CRITICAL WARNING ⚠️                           ║");
        log.warn("║                     Starter Entities Will NOT Be Discovered!                  ║");
        log.warn("╚═══════════════════════════════════════════════════════════════════════════════╝");
        log.warn("");
        log.warn("Your application uses @EntityScan which creates an EntityScanPackages bean.");
        log.warn("Hibernate will scan ONLY the packages in EntityScanPackages and IGNORE");
        log.warn("AutoConfigurationPackages (which includes the starter entity package).");
        log.warn("");
        log.warn("Current EntityScanPackages:");
        existingPackages.forEach(pkg -> log.warn("  • {}", pkg));
        log.warn("");
        log.warn("Missing: com.galoong.aiprompttracker.domain.entity");
        log.warn("");
        log.warn("═══════════════════════════════════════════════════════════════════════════════");
        log.warn("SOLUTION: Add starter entity package to your @EntityScan:");
        log.warn("═══════════════════════════════════════════════════════════════════════════════");
        log.warn("");
        log.warn("@SpringBootApplication");
        log.warn("@EntityScan(basePackages = {");
        existingPackages.forEach(pkg -> log.warn("    \"{}\",", pkg));
        log.warn("    \"com.galoong.aiprompttracker.domain.entity\"  // Add this line!");
        log.warn("})");
        log.warn("public class YourApplication {");
        log.warn("    public static void main(String[] args) {");
        log.warn("        SpringApplication.run(YourApplication.class, args);");
        log.warn("    }");
        log.warn("}");
        log.warn("");
        log.warn("═══════════════════════════════════════════════════════════════════════════════");
        log.warn("IMPACT:");
        log.warn("  • Starter entities (Execution, Call) will NOT be managed by JPA");
        log.warn("  • Starter repositories (ExecutionRepository, CallRepository) will fail");
        log.warn("  • Application may fail at runtime if persistence mode requires starter entities");
        log.warn("═══════════════════════════════════════════════════════════════════════════════");
        log.warn("");
    }
}
