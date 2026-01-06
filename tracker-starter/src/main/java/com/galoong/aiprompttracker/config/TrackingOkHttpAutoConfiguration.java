package com.galoong.aiprompttracker.config;

import com.galoong.aiprompttracker.config.properties.TrackingCaptureProperties;
import com.galoong.aiprompttracker.tracking.classifier.ModelExtractor;
import com.galoong.aiprompttracker.tracking.classifier.ProviderClassifier;
import com.galoong.aiprompttracker.tracking.collector.CallCollector;
import com.galoong.aiprompttracker.tracking.interceptor.TrackingOkHttpInterceptor;
import com.galoong.aiprompttracker.tracking.interceptor.UsageMetricsParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for OkHttp tracking support.
 *
 * <p>This configuration is isolated to prevent ClassNotFoundException
 * when OkHttp is not on the classpath.
 */
@Slf4j
@Configuration
@ConditionalOnClass(name = "okhttp3.Interceptor")
public class TrackingOkHttpAutoConfiguration {

    /**
     * OkHttp tracking interceptor bean (for manual registration)
     *
     * <p>Users must manually add this to their OkHttpClient:
     * <pre>
     * OkHttpClient client = new OkHttpClient.Builder()
     *     .addInterceptor(trackingOkHttpInterceptor)
     *     .build();
     * </pre>
     */
    @Bean
    @ConditionalOnMissingBean
    public TrackingOkHttpInterceptor trackingOkHttpInterceptor(
            ProviderClassifier providerClassifier,
            ModelExtractor modelExtractor,
            CallCollector callCollector,
            UsageMetricsParser usageMetricsParser,
            TrackingCaptureProperties captureProperties) {

        log.info("Registering TrackingOkHttpInterceptor bean (requires manual OkHttpClient setup)");
        return new TrackingOkHttpInterceptor(
                providerClassifier,
                modelExtractor,
                callCollector,
                usageMetricsParser,
                captureProperties
        );
    }
}
