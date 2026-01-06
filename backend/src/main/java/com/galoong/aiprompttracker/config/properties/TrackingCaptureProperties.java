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
     * Master switch: enable/disable request/response body capture.
     *
     * If false: NEVER attempt body capture. Track only metadata (provider, model, latency, status).
     * If true: Attempt capture according to captureMode rules.
     *
     * Default: false (safe, zero risk, metadata-only tracking)
     */
    private boolean captureEnabled = false;

    /**
     * Capture mode: controls safety vs. completeness tradeoff.
     *
     * SAFE (default): Only capture when all preconditions met (content-length present, within limits, allowed content-type).
     *                 Guarantees no "body already consumed" errors and perfect downstream fidelity.
     *
     * FORCE (opt-in): Best-effort capture even when preconditions not met. May truncate large responses.
     *                 Use only if you accept potential downstream truncation in edge cases.
     */
    private CaptureMode captureMode = CaptureMode.SAFE;

    /**
     * Enable storing raw request/response data.
     * Should be true in dev/test, false in production for privacy/storage.
     *
     * Note: This property only takes effect when captureEnabled=true.
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

    /**
     * Capture mode enum
     */
    public enum CaptureMode {
        /**
         * SAFE mode: Only capture when all preconditions are met.
         * - Content-Type is allowed
         * - Content-Length header is present
         * - Content-Length <= maxInMemoryBytes
         * - storeRawData == true
         *
         * Guarantees: No "body already consumed" errors, perfect downstream fidelity.
         */
        SAFE,

        /**
         * FORCE mode: Best-effort capture even when preconditions aren't met.
         * - Attempts capture even with missing Content-Length or large bodies
         * - May truncate responses that exceed limits
         * - Logs warnings when truncation occurs
         *
         * Use only if you accept potential downstream truncation in edge cases.
         */
        FORCE
    }
}
