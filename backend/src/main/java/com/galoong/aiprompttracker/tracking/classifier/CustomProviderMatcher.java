package com.galoong.aiprompttracker.tracking.classifier;

/**
 * Extension point for custom AI provider detection.
 *
 * <p>Implement this interface and register as a Spring bean to add support
 * for additional AI providers not included by default.
 *
 * <p>Example:
 * <pre>
 * &#64;Component
 * public class MyCustomProviderMatcher implements CustomProviderMatcher {
 *     &#64;Override
 *     public String matchProvider(String host, String path) {
 *         if (host.contains("api.mycustom.ai")) {
 *             return "MyCustomAI";
 *         }
 *         return null; // No match
 *     }
 *
 *     &#64;Override
 *     public int getOrder() {
 *         return 0; // Higher priority than built-in matchers
 *     }
 * }
 * </pre>
 */
public interface CustomProviderMatcher {

    /**
     * Attempt to match and classify the provider based on host and path
     *
     * @param host The HTTP host (e.g., "api.example.com")
     * @param path The HTTP path (e.g., "/v1/completions")
     * @return Provider name if matched, or null if not recognized
     */
    String matchProvider(String host, String path);

    /**
     * Order for this matcher (lower = higher priority)
     *
     * <p>Custom matchers with lower order will be checked before built-in matchers.
     * Default built-in matcher order is 100.
     *
     * @return Order value (default: 100)
     */
    default int getOrder() {
        return 100;
    }
}
