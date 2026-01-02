package com.galoong.aiprompttracker.tracking.classifier;

import org.springframework.stereotype.Component;

/**
 * Classifies AI provider based on HTTP request host and path.
 * Supports OpenAI, Anthropic, Google, and can be extended for others.
 */
@Component
public class ProviderClassifier {

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
