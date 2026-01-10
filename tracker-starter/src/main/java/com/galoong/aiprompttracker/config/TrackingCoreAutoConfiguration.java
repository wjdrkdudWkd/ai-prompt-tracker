package com.galoong.aiprompttracker.config;

import com.galoong.aiprompttracker.config.properties.TrackingCaptureProperties;
import com.galoong.aiprompttracker.tracking.classifier.ModelExtractor;
import com.galoong.aiprompttracker.tracking.classifier.ProviderClassifier;
import com.galoong.aiprompttracker.tracking.collector.CallCollector;
import com.galoong.aiprompttracker.tracking.interceptor.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Core auto-configuration for AI Prompt Tracker.
 *
 * <p>Registers core tracking components (always active):
 * <ul>
 *   <li>AOP aspect for @AIPrompt methods</li>
 *   <li>HTTP client interceptors (WebClient, RestTemplate, RestClient, OkHttp)</li>
 *   <li>Provider classifiers and parsers</li>
 *   <li>Tracking context management</li>
 * </ul>
 *
 * <p>All interceptors only activate when inside @AIPrompt execution context.
 *
 * <p>Persistence is handled separately by {@link TrackingPersistenceAutoConfiguration}.
 *
 * <p><b>Important:</b> All beans are registered explicitly here to avoid relying on
 * component scanning in consumer projects.
 */
@Slf4j
@Configuration
public class TrackingCoreAutoConfiguration {

    // ========== Core Tracking Beans ==========

    /**
     * TrackingContext - manages thread-local execution context
     */
    @Bean
    @ConditionalOnMissingBean
    public com.galoong.aiprompttracker.tracking.context.TrackingContext trackingContext(
            com.galoong.aiprompttracker.tracking.storage.ExecutionStore executionStore) {
        log.info("Registering TrackingContext (thread-local execution context manager)");
        com.galoong.aiprompttracker.tracking.context.TrackingContext context =
                new com.galoong.aiprompttracker.tracking.context.TrackingContext();
        context.setExecutionStore(executionStore);
        return context;
    }

    /**
     * ProviderClassifier - classifies AI providers from HTTP host/path
     *
     * <p>Accepts optional List of CustomProviderMatcher beans for extensibility
     */
    @Bean
    @ConditionalOnMissingBean
    public ProviderClassifier providerClassifier(
            java.util.List<com.galoong.aiprompttracker.tracking.classifier.CustomProviderMatcher> customMatchers) {
        log.info("Registering ProviderClassifier with {} custom matcher(s)", customMatchers.size());
        return new ProviderClassifier(customMatchers);
    }

    /**
     * ModelExtractor - extracts model names from request/response
     */
    @Bean
    @ConditionalOnMissingBean
    public ModelExtractor modelExtractor(com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        log.info("Registering ModelExtractor");
        return new ModelExtractor(objectMapper);
    }

    /**
     * UsageMetricsParser - parses usage metrics from AI responses
     */
    @Bean
    @ConditionalOnMissingBean
    public UsageMetricsParser usageMetricsParser(com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        log.info("Registering UsageMetricsParser");
        return new UsageMetricsParser(objectMapper);
    }

    /**
     * DefaultCallCollector - collects call data
     */
    @Bean
    @ConditionalOnMissingBean(CallCollector.class)
    public com.galoong.aiprompttracker.tracking.collector.DefaultCallCollector defaultCallCollector(
            com.galoong.aiprompttracker.tracking.storage.CallStore callStore) {
        log.info("Registering DefaultCallCollector");
        return new com.galoong.aiprompttracker.tracking.collector.DefaultCallCollector(callStore);
    }

    // ========== HTTP Client Interceptors ==========

    /**
     * WebClient tracking filter (already exists, ensure it's a bean)
     */
    @Bean
    @ConditionalOnClass(WebClient.class)
    @ConditionalOnMissingBean
    public TrackingWebClientFilter trackingWebClientFilter(
            ProviderClassifier providerClassifier,
            ModelExtractor modelExtractor,
            CallCollector callCollector,
            UsageMetricsParser usageMetricsParser,
            TrackingCaptureProperties captureProperties) {

        log.info("Registering TrackingWebClientFilter for WebClient support");
        return new TrackingWebClientFilter(
                providerClassifier,
                modelExtractor,
                callCollector,
                usageMetricsParser,
                captureProperties
        );
    }

    /**
     * WebClient.Builder customizer to auto-register filter globally
     *
     * <p>This ensures Spring AI and other WebClient users automatically get tracking
     */
    @Bean
    @ConditionalOnClass(WebClient.class)
    public org.springframework.boot.web.reactive.function.client.WebClientCustomizer trackingWebClientCustomizer(
            TrackingWebClientFilter trackingWebClientFilter) {

        log.info("Registering WebClientCustomizer to auto-apply tracking filter");
        return webClientBuilder -> webClientBuilder.filter(trackingWebClientFilter);
    }

    /**
     * RestTemplate tracking interceptor
     */
    @Bean
    @ConditionalOnClass(RestTemplate.class)
    @ConditionalOnMissingBean
    public TrackingRestTemplateInterceptor trackingRestTemplateInterceptor(
            ProviderClassifier providerClassifier,
            ModelExtractor modelExtractor,
            CallCollector callCollector,
            UsageMetricsParser usageMetricsParser,
            TrackingCaptureProperties captureProperties) {

        log.info("Registering TrackingRestTemplateInterceptor for RestTemplate support");
        return new TrackingRestTemplateInterceptor(
                providerClassifier,
                modelExtractor,
                callCollector,
                usageMetricsParser,
                captureProperties
        );
    }

    /**
     * RestTemplate customizer to auto-register interceptor globally
     */
    @Bean
    @ConditionalOnClass(RestTemplate.class)
    public RestTemplateCustomizer trackingRestTemplateCustomizer(
            TrackingRestTemplateInterceptor trackingRestTemplateInterceptor) {

        log.info("Registering RestTemplateCustomizer to auto-apply tracking interceptor");
        return restTemplate -> restTemplate.getInterceptors().add(trackingRestTemplateInterceptor);
    }

    /**
     * RestClient.Builder customizer (Spring Boot 3)
     */
    @Bean
    @ConditionalOnClass(RestClient.class)
    public org.springframework.boot.web.client.RestClientCustomizer trackingRestClientCustomizer(
            TrackingRestTemplateInterceptor trackingRestTemplateInterceptor) {

        log.info("Registering RestClientCustomizer to auto-apply tracking interceptor");
        return restClientBuilder -> restClientBuilder
                .requestInterceptor(trackingRestTemplateInterceptor);
    }
}
