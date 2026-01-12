package com.galoong.aiprompttracker.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for JPA entity and repository scanning.
 *
 * <p><b>CRITICAL - Early Package Registration + Dual-Phase Warning:</b>
 * This configuration provides:
 * <ol>
 *   <li>{@link AiPromptTrackerAutoConfigPackageRegistrar} - Registers starter package to AutoConfigurationPackages (early)</li>
 *   <li>{@link AiPromptTrackerJpaEntityScanWarningRegistrar} - Early-phase EntityScan detection (ImportBeanDefinitionRegistrar)</li>
 *   <li>{@link EntityScanWarningBeanFactoryPostProcessor} - Late-phase EntityScan detection (BeanFactoryPostProcessor)</li>
 * </ol>
 *
 * <p><b>Dual-Phase EntityScan Warning Detection:</b>
 * <ul>
 *   <li><b>Phase 1 (Early):</b> ImportBeanDefinitionRegistrar attempts to detect EntityScanPackages early</li>
 *   <li><b>Limitation:</b> May miss EntityScanPackages if not yet registered (timing dependent)</li>
 *   <li><b>Phase 2 (Late):</b> BeanFactoryPostProcessor guarantees detection after all beans registered</li>
 *   <li><b>Duplicate Prevention:</b> {@link EntityScanWarningState} ensures only ONE warning emitted</li>
 * </ul>
 *
 * <p><b>Why ImportBeanDefinitionRegistrar for Package Registration?</b>
 * <ul>
 *   <li><b>Timing</b>: Runs EARLY during @Configuration class processing</li>
 *   <li><b>BEFORE repository scanning</b>: Repository scanning sees the updated AutoConfigurationPackages</li>
 *   <li><b>BEFORE entity scanning</b>: Packages registered before Hibernate starts</li>
 *   <li><b>NOT BeanFactoryPostProcessor</b>: BFPP runs too late for package registration</li>
 * </ul>
 *
 * <p><b>Strategy - AutoConfigurationPackages Extension:</b>
 * <ul>
 *   <li>Consumer's @SpringBootApplication registers consumer package in AutoConfigurationPackages</li>
 *   <li>Our registrar APPENDS starter package to AutoConfigurationPackages</li>
 *   <li>Spring Data JPA scans AutoConfigurationPackages → finds all repositories</li>
 *   <li>Hibernate scans AutoConfigurationPackages (when EntityScanPackages not set) → finds all entities</li>
 *   <li>Result: Both consumer and starter entities/repositories are discovered</li>
 * </ul>
 *
 * <p><b>Why NOT @EnableJpaRepositories?</b>
 * Using @EnableJpaRepositories in a starter causes Spring Boot's JpaRepositoriesAutoConfiguration
 * to back off completely, breaking consumer repository scanning.
 *
 * <p><b>Why NOT @EntityScan?</b>
 * <ul>
 *   <li>If we use @EntityScan, we create EntityScanPackages</li>
 *   <li>Hibernate then scans ONLY EntityScanPackages (ignoring AutoConfigurationPackages)</li>
 *   <li>Consumer entities would fail with "Not a managed type"</li>
 *   <li>Solution: Never create EntityScanPackages, let Hibernate use AutoConfigurationPackages</li>
 * </ul>
 *
 * <p><b>Ordering:</b>
 * <ul>
 *   <li>{@code @AutoConfigureBefore(JpaRepositoriesAutoConfiguration.class)} - Run before repository scanning</li>
 *   <li>{@code @AutoConfigureBefore(HibernateJpaAutoConfiguration.class)} - Run before entity manager creation</li>
 * </ul>
 *
 * <p><b>Consumer with explicit @EntityScan:</b>
 * If consumer uses @EntityScan, they create EntityScanPackages which overrides AutoConfigurationPackages
 * for entity scanning. In this case, consumer MUST include starter entity package:
 * <pre>
 * {@code @SpringBootApplication}
 * {@code @EntityScan(basePackages = {}
 *     "com.yourcompany.yourapp.domain",
 *     "com.galoong.aiprompttracker.domain.entity"
 * })
 * public class YourApplication {}
 * </pre>
 *
 * See {@link EntityScanWarningState} and dual-phase detection mechanisms for warnings.
 *
 * @see AiPromptTrackerAutoConfigPackageRegistrar
 * @see AiPromptTrackerJpaEntityScanWarningRegistrar
 * @see EntityScanWarningBeanFactoryPostProcessor
 * @see EntityScanWarningState
 * @see org.springframework.boot.autoconfigure.AutoConfigurationPackages
 * @see JpaRepositoriesAutoConfiguration
 * @see HibernateJpaAutoConfiguration
 */
@Slf4j
@AutoConfiguration
@AutoConfigureBefore({
        JpaRepositoriesAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
@Import({
        AiPromptTrackerAutoConfigPackageRegistrar.class,      // Register starter package early
        AiPromptTrackerJpaEntityScanWarningRegistrar.class    // Early-phase EntityScan warning detection
})
public class AiPromptTrackerJpaScanAutoConfiguration {

    public AiPromptTrackerJpaScanAutoConfiguration() {
        log.info("AI Prompt Tracker: JPA scan bootstrap loaded (AutoConfigurationPackages extension + dual-phase warning)");
    }

    /**
     * Registers a late-running {@link BeanFactoryPostProcessor} for reliable EntityScan detection.
     *
     * <p>This BFPP runs with {@link org.springframework.core.Ordered#LOWEST_PRECEDENCE} to ensure
     * all bean definitions are loaded before checking for EntityScanPackages.
     *
     * <p><b>Why Static Method:</b>
     * Static @Bean methods are processed earlier and don't require instantiation of the
     * configuration class, ensuring proper ordering.
     *
     * @return EntityScan warning BFPP
     */
    @Bean
    public static BeanFactoryPostProcessor entityScanWarningBeanFactoryPostProcessor() {
        return new EntityScanWarningBeanFactoryPostProcessor();
    }
}
