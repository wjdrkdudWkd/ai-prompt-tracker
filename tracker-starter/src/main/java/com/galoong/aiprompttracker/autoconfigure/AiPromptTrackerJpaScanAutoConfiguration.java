package com.galoong.aiprompttracker.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for JPA entity and repository scanning.
 *
 * <p><b>CRITICAL - Early Package Registration + Warning:</b>
 * This configuration imports two {@link org.springframework.context.annotation.ImportBeanDefinitionRegistrar}s:
 * <ol>
 *   <li>{@link AiPromptTrackerAutoConfigPackageRegistrar} - Registers starter package to AutoConfigurationPackages</li>
 *   <li>{@link AiPromptTrackerJpaEntityScanWarningRegistrar} - Warns if explicit @EntityScan excludes starter package</li>
 * </ol>
 *
 * <p><b>Why ImportBeanDefinitionRegistrar?</b>
 * <ul>
 *   <li><b>Timing</b>: Runs EARLY during @Configuration class processing</li>
 *   <li><b>BEFORE repository scanning</b>: Repository scanning sees the updated AutoConfigurationPackages</li>
 *   <li><b>BEFORE entity scanning</b>: Can detect EntityScanPackages and warn consumer</li>
 *   <li><b>NOT BeanFactoryPostProcessor</b>: BFPP runs too late (after repository scanning)</li>
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
 * See {@link AiPromptTrackerJpaConsumerWarningsAutoConfiguration} for runtime detection and warnings.
 *
 * @see AiPromptTrackerAutoConfigPackageRegistrar
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
        AiPromptTrackerJpaEntityScanWarningRegistrar.class    // Warn if explicit @EntityScan missing starter package
})
public class AiPromptTrackerJpaScanAutoConfiguration {

    public AiPromptTrackerJpaScanAutoConfiguration() {
        log.info("AI Prompt Tracker: JPA scan bootstrap loaded (AutoConfigurationPackages extension)");
    }
}
