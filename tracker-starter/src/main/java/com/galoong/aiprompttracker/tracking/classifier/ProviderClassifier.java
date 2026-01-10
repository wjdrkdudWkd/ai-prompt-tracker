package com.galoong.aiprompttracker.tracking.classifier;

import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;

/**
 * Classifies AI provider based on HTTP request host and path.
 *
 * <p>Supports OpenAI, Anthropic, Google, Cohere, Mistral, and others by default.
 * Can be extended via {@link CustomProviderMatcher} beans.
 *
 * <p>Custom matchers are checked first (ordered by {@link CustomProviderMatcher#getOrder()}),
 * then built-in matchers are used as fallback.
 *
 * <p><b>Note:</b> This class is registered as a bean by TrackingCoreAutoConfiguration.
 * Do not use @Component annotation to avoid relying on component scanning.
 */
@Slf4j
public class ProviderClassifier {

    private final List<CustomProviderMatcher> customMatchers;

    /**
     * Constructor with optional custom matchers
     *
     * @param customMatchers List of custom provider matchers (can be empty)
     */
    public ProviderClassifier(List<CustomProviderMatcher> customMatchers) {
        this.customMatchers = customMatchers;
        // Sort by order (lower = higher priority)
        this.customMatchers.sort(Comparator.comparingInt(CustomProviderMatcher::getOrder));

        if (!customMatchers.isEmpty()) {
            log.info("Registered {} custom provider matcher(s)", customMatchers.size());
        }
    }

    /**
     * Classify the AI provider based on the request host and path
     *
     * @param host The HTTP host (e.g., "api.openai.com")
     * @param path The HTTP path (e.g., "/v1/chat/completions")
     * @return The provider name (e.g., "OpenAI", "Anthropic", "Google", or "Unknown")
     */
    public String classifyProvider(String host, String path) {
        if (host == null) {
            return "Unknown";
        }

        // Try custom matchers first (ordered by priority)
        for (CustomProviderMatcher matcher : customMatchers) {
            String provider = matcher.matchProvider(host, path);
            if (provider != null) {
                log.debug("Custom matcher detected provider: {}", provider);
                return provider;
            }
        }

        // Fallback to built-in matchers
        return classifyBuiltInProvider(host, path);
    }

    /**
     * Built-in provider classification
     */
    private String classifyBuiltInProvider(String host, String path) {
        String lowerHost = host.toLowerCase();

        // OpenAI
        if (lowerHost.contains("api.openai.com") || lowerHost.contains("openai.azure.com")) {
            return "OpenAI";
        }

        // Anthropic (Claude)
        if (lowerHost.contains("api.anthropic.com")) {
            return "Anthropic";
        }

        // Google (Gemini / Vertex AI)
        if (lowerHost.contains("generativelanguage.googleapis.com") ||
            lowerHost.contains("aiplatform.googleapis.com")) {
            return "Google";
        }

        // Cohere
        if (lowerHost.contains("api.cohere.ai") || lowerHost.contains("cohere.com")) {
            return "Cohere";
        }

        // Mistral AI
        if (lowerHost.contains("api.mistral.ai")) {
            return "Mistral";
        }

        // Hugging Face
        if (lowerHost.contains("api-inference.huggingface.co")) {
            return "HuggingFace";
        }

        // Perplexity
        if (lowerHost.contains("api.perplexity.ai")) {
            return "Perplexity";
        }

        return "Unknown";
    }

    /**
     * Check if a host is a known AI provider
     */
    public boolean isKnownProvider(String host) {
        return !"Unknown".equals(classifyProvider(host, null));
    }
}
