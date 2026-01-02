package com.galoong.aiprompttracker.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Configuration properties for AI call tracking and capture behavior.
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai-prompts.tracking")
public class TrackingCaptureProperties {

    /**
     * Enable storing raw request/response data.
     * Should be true in dev/test, false in production for privacy/storage.
     */
    private boolean storeRawData = true;

    /**
     * Maximum bytes to capture from request body.
     * Default: 16KB (sufficient for most AI API requests)
     */
    private int maxRequestBytes = 16384;

    /**
     * Maximum bytes to capture from response body.
     * Default: 32KB (AI responses can be longer)
     */
    private int maxResponseBytes = 32768;

    /**
     * Hard cap for total in-memory buffering (emergency limit to prevent OOM).
     * If response body exceeds this, we skip full body reconstruction.
     * Default: 2MB (2097152 bytes)
     */
    private int maxInMemoryBytes = 2097152;

    /**
     * Content types to capture (case-insensitive contains check).
     * Only JSON content is typically needed for AI APIs.
     */
    private Set<String> captureContentTypes = Set.of("application/json");

    /**
     * Whether to capture calls to unknown providers.
     * Default: false (only track known AI providers)
     */
    private boolean captureUnknownProviders = false;

    /**
     * Truncation suffix appended when content exceeds max bytes
     */
    private String truncationSuffix = " ... (truncated)";

    /**
     * Check if a content type should be captured
     */
    public boolean shouldCaptureContentType(String contentType) {
        if (contentType == null || captureContentTypes.isEmpty()) {
            return false;
        }

        String lowerContentType = contentType.toLowerCase();
        return captureContentTypes.stream()
                .anyMatch(allowed -> lowerContentType.contains(allowed.toLowerCase()));
    }
}
