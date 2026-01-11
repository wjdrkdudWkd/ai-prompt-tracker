package com.galoong.aiprompttracker.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Registers the AI Prompt Tracker base package into Spring Boot's AutoConfigurationPackages.
 *
 * <p><b>Purpose:</b> This enables Spring Boot's default JPA repository scanning to automatically
 * discover the starter's repositories without requiring consumer-side {@code @EnableJpaRepositories}
 * configuration.
 *
 * <p><b>How It Works:</b>
 * <ol>
 *   <li>Spring Boot's {@code JpaRepositoriesAutoConfiguration} scans packages registered in
 *       {@code AutoConfigurationPackages}</li>
 *   <li>By default, it scans the main application's base package</li>
 *   <li>This registrar adds {@code com.galoong.aiprompttracker} to the scan list</li>
 *   <li>Result: Both consumer and starter repositories are discovered automatically</li>
 * </ol>
 *
 * <p><b>Benefits:</b>
 * <ul>
 *   <li>Consumer repositories continue to work (no interference)</li>
 *   <li>Starter repositories are auto-discovered (no consumer configuration needed)</li>
 *   <li>True plug-and-play experience</li>
 *   <li>Follows Spring Boot auto-configuration best practices</li>
 * </ul>
 *
 * <p><b>Why Not {@code @EnableJpaRepositories}?</b>
 * Using {@code @EnableJpaRepositories} in a starter causes Spring Boot's
 * {@code JpaRepositoriesAutoConfiguration} to completely back off, breaking consumer repository
 * scanning. The AutoConfigurationPackages approach extends (rather than replaces) Boot's
 * default scanning.
 *
 * @see AutoConfigurationPackages
 * @see org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration
 */
@Slf4j
public class AiPromptTrackerAutoConfigPackageRegistrar implements ImportBeanDefinitionRegistrar {

    /**
     * Registers the starter's base package into AutoConfigurationPackages.
     *
     * @param importingClassMetadata metadata of the importing class
     * @param registry the bean definition registry
     */
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                        BeanDefinitionRegistry registry) {
        // Register the starter's base package for automatic scanning
        AutoConfigurationPackages.register(registry, "com.galoong.aiprompttracker");

        log.debug("AI Prompt Tracker: Registered auto-configuration base package 'com.galoong.aiprompttracker' for repository scanning");
    }
}
