package com.galoong.aiprompttracker.config;

import com.galoong.aiprompttracker.tracking.interceptor.TrackingWebClientFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient configuration with tracking filter.
 * Users can inject this WebClient bean to get automatic tracking.
 */
@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private final TrackingWebClientFilter trackingFilter;

    /**
     * WebClient with automatic AI call tracking
     */
    @Bean
    public WebClient trackedWebClient() {
        return WebClient.builder()
                .filter(trackingFilter)
                .build();
    }

    /**
     * Alternative: WebClient.Builder with tracking filter
     * Users can customize further after injection
     */
    @Bean
    public WebClient.Builder trackedWebClientBuilder() {
        return WebClient.builder()
                .filter(trackingFilter);
    }
}
