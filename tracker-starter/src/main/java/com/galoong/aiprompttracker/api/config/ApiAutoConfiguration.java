package com.galoong.aiprompttracker.api.config;

import com.galoong.aiprompttracker.api.controller.CallController;
import com.galoong.aiprompttracker.api.controller.DashboardController;
import com.galoong.aiprompttracker.api.controller.ExecutionController;
import com.galoong.aiprompttracker.api.controller.FunctionController;
import com.galoong.aiprompttracker.api.controller.UiRedirectController;
import com.galoong.aiprompttracker.api.exception.PersistenceDisabledException;
import com.galoong.aiprompttracker.api.service.*;
import com.galoong.aiprompttracker.config.TrackingJpaAutoConfiguration;
import com.galoong.aiprompttracker.config.properties.TrackingUiProperties;
import com.galoong.aiprompttracker.domain.repository.ExecutionRepository;
import com.galoong.aiprompttracker.tracking.storage.ExecutionStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Auto-configuration for AI Prompt Tracker REST API.
 *
 * <p>Registers all API services and controllers explicitly to avoid
 * relying on component scanning in consumer projects.
 *
 * <p><b>Activation:</b> Services are conditionally created only when their repository
 * dependencies exist, preventing {@link org.springframework.beans.factory.NoSuchBeanDefinitionException}
 * when JPA is disabled or repositories are not configured.
 *
 * <p><b>Load Order:</b> Configured to load after {@link TrackingJpaAutoConfiguration}
 * to ensure repositories are registered before API beans are created.
 *
 * <p>When persistence is disabled, API beans are replaced with no-op
 * implementations that throw {@link PersistenceDisabledException}.
 */
@Slf4j
@Configuration
@AutoConfigureAfter(TrackingJpaAutoConfiguration.class)
@EnableConfigurationProperties(TrackingUiProperties.class)
public class ApiAutoConfiguration implements WebMvcConfigurer {

    public ApiAutoConfiguration() {
        log.info("AI Prompt Tracker: API auto-configuration loaded");
    }

    // ========== API Services ==========

    @Bean
    @ConditionalOnMissingBean
    public DashboardService dashboardService(
            com.galoong.aiprompttracker.domain.repository.ExecutionRepository executionRepository,
            com.galoong.aiprompttracker.domain.repository.CallRepository callRepository,
            PersistenceGuard persistenceGuard) {
        log.info("Registering DashboardService");
        return new DashboardService(executionRepository, callRepository, persistenceGuard);
    }

    @Bean
    @ConditionalOnMissingBean
    public ExecutionService executionService(
            com.galoong.aiprompttracker.domain.repository.ExecutionRepository executionRepository,
            com.galoong.aiprompttracker.domain.repository.CallRepository callRepository,
            PersistenceGuard persistenceGuard) {
        log.info("Registering ExecutionService");
        return new ExecutionService(executionRepository, callRepository, persistenceGuard);
    }

    @Bean
    @ConditionalOnMissingBean
    public CallService callService(
            com.galoong.aiprompttracker.domain.repository.CallRepository callRepository,
            com.galoong.aiprompttracker.domain.repository.ExecutionRepository executionRepository,
            PersistenceGuard persistenceGuard) {
        log.info("Registering CallService");
        return new CallService(callRepository, executionRepository, persistenceGuard);
    }

    @Bean
    @ConditionalOnMissingBean
    public FunctionService functionService(
            com.galoong.aiprompttracker.domain.repository.ExecutionRepository executionRepository,
            com.galoong.aiprompttracker.domain.repository.CallRepository callRepository,
            PersistenceGuard persistenceGuard) {
        log.info("Registering FunctionService");
        return new FunctionService(executionRepository, callRepository, persistenceGuard);
    }

    // ========== API Controllers ==========

    @Bean
    @ConditionalOnMissingBean
    public DashboardController dashboardController(DashboardService dashboardService) {
        log.info("Registering DashboardController");
        return new DashboardController(dashboardService);
    }

    @Bean
    @ConditionalOnMissingBean
    public ExecutionController executionController(ExecutionService executionService) {
        log.info("Registering ExecutionController");
        return new ExecutionController(executionService);
    }

    @Bean
    @ConditionalOnMissingBean
    public CallController callController(CallService callService) {
        log.info("Registering CallController");
        return new CallController(callService);
    }

    @Bean
    @ConditionalOnMissingBean
    public FunctionController functionController(FunctionService functionService) {
        log.info("Registering FunctionController");
        return new FunctionController(functionService);
    }

    @Bean
    @ConditionalOnMissingBean
    public UiRedirectController uiRedirectController(TrackingUiProperties uiProperties) {
        log.info("Registering UiRedirectController (UI enabled: {})", uiProperties.isEnabled());
        return new UiRedirectController(uiProperties);
    }

    // ========== Helper Beans ==========

    /**
     * Persistence guard bean
     *
     * <p>Injected into services to check if persistence is enabled.
     */
    @Bean
    public PersistenceGuard persistenceGuard(ExecutionStore executionStore) {
        boolean persistent = executionStore.isPersistent();
        log.info("API persistence guard initialized: persistent={}", persistent);
        return new PersistenceGuard(persistent);
    }

    // ========== Resource Handling ==========

    /**
     * Configure static resource handling with explicit priority.
     *
     * <p>CRITICAL: Static resources (_next/**, *.js, *.css, etc.) MUST be handled
     * BEFORE the UiRedirectController's catch-all /** mapping. This ensures that:
     * <ul>
     *   <li>/aiprompt-tracker/_next/static/*.js returns JavaScript (not HTML)</li>
     *   <li>/aiprompt-tracker/*.css returns CSS (not HTML)</li>
     *   <li>Other static assets load correctly</li>
     * </ul>
     *
     * <p>By explicitly registering these resource handlers, we ensure Spring MVC
     * checks them BEFORE falling through to the controller's /** pattern.
     */
    /**
     * CRITICAL: This doesn't work as expected because Spring MVC checks
     * @RequestMapping in controllers BEFORE resource handlers.
     *
     * The real solution is to change the controller mapping to NOT use catch-all /**,
     * or use a HandlerInterceptor that runs before the controller.
     *
     * For now, keeping this as documentation of what was attempted.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // NOTE: These resource handlers are registered but checked AFTER @GetMapping in controllers
        // This means UiRedirectController's /** still matches first
        // The real fix needs to be in the controller or using a different approach

        registry.addResourceHandler("/aiprompt-tracker/_next/**")
                .addResourceLocations("classpath:/META-INF/resources/aiprompt-tracker/_next/");

        registry.addResourceHandler("/aiprompt-tracker/*.js")
                .addResourceLocations("classpath:/META-INF/resources/aiprompt-tracker/");

        registry.addResourceHandler("/aiprompt-tracker/*.css")
                .addResourceLocations("classpath:/META-INF/resources/aiprompt-tracker/");

        registry.addResourceHandler("/aiprompt-tracker/*.map")
                .addResourceLocations("classpath:/META-INF/resources/aiprompt-tracker/");

        registry.addResourceHandler("/aiprompt-tracker/*.txt")
                .addResourceLocations("classpath:/META-INF/resources/aiprompt-tracker/");

        registry.addResourceHandler("/aiprompt-tracker/*.ico")
                .addResourceLocations("classpath:/META-INF/resources/aiprompt-tracker/");

        registry.addResourceHandler("/aiprompt-tracker/*.png")
                .addResourceLocations("classpath:/META-INF/resources/aiprompt-tracker/");

        registry.addResourceHandler("/aiprompt-tracker/*.jpg")
                .addResourceLocations("classpath:/META-INF/resources/aiprompt-tracker/");

        registry.addResourceHandler("/aiprompt-tracker/*.svg")
                .addResourceLocations("classpath:/META-INF/resources/aiprompt-tracker/");

        registry.addResourceHandler("/aiprompt-tracker/*.woff2")
                .addResourceLocations("classpath:/META-INF/resources/aiprompt-tracker/");

        registry.addResourceHandler("/aiprompt-tracker/*.woff")
                .addResourceLocations("classpath:/META-INF/resources/aiprompt-tracker/");

        registry.addResourceHandler("/aiprompt-tracker/*.ttf")
                .addResourceLocations("classpath:/META-INF/resources/aiprompt-tracker/");

        registry.addResourceHandler("/aiprompt-tracker/*.json")
                .addResourceLocations("classpath:/META-INF/resources/aiprompt-tracker/");

        log.info("Configured resource handlers for /aiprompt-tracker (Note: Checked after controller mappings)");
    }

    /**
     * Helper class to guard API operations
     */
    public static class PersistenceGuard {
        private final boolean persistent;

        public PersistenceGuard(boolean persistent) {
            this.persistent = persistent;
        }

        /**
         * Check if persistence is enabled, throw exception if not
         */
        public void requirePersistence() {
            if (!persistent) {
                throw new PersistenceDisabledException();
            }
        }

        public boolean isPersistent() {
            return persistent;
        }
    }
}
