package com.galoong.aiprompttracker.tracking.classifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Extracts model name from AI API request bodies.
 * Handles different provider formats.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelExtractor {

    private final ObjectMapper objectMapper;

    /**
     * Extract model name from request body JSON
     *
     * @param requestBody The JSON request body
     * @param provider The provider name (for provider-specific parsing)
     * @return The model name or "unknown" if not found
     */
    public String extractModel(String requestBody, String provider) {
        if (requestBody == null || requestBody.isEmpty()) {
            return "unknown";
        }

        try {
            JsonNode json = objectMapper.readTree(requestBody);

            // Most providers use "model" field (OpenAI, Anthropic, Cohere, Mistral)
            if (json.has("model")) {
                return json.get("model").asText();
            }

            // Google Vertex AI sometimes uses different structure
            if ("Google".equals(provider)) {
                // Check for model in path or other locations
                if (json.has("contents") && json.has("generationConfig")) {
                    // Model is usually in the API endpoint for Google
                    return "gemini-pro"; // TODO: Extract from URL instead
                }
            }

            // Hugging Face uses "model" in the URL, not body
            if ("HuggingFace".equals(provider)) {
                return "hf-model"; // TODO: Extract from URL
            }

            log.debug("Model field not found in request body for provider: {}", provider);
            return "unknown";

        } catch (Exception e) {
            log.warn("Failed to parse request body for model extraction", e);
            return "unknown";
        }
    }

    /**
     * Extract model from URL path (for providers that use path-based model specification)
     *
     * @param path The URL path
     * @param provider The provider name
     * @return The model name or null if not found
     */
    public String extractModelFromPath(String path, String provider) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        // Google Gemini: /v1beta/models/gemini-pro:generateContent
        if ("Google".equals(provider) && path.contains("/models/")) {
            int startIdx = path.indexOf("/models/") + 8;
            int endIdx = path.indexOf(":", startIdx);
            if (endIdx == -1) {
                endIdx = path.length();
            }
            if (startIdx < endIdx) {
                return path.substring(startIdx, endIdx);
            }
        }

        // HuggingFace: /models/{model-id}
        if ("HuggingFace".equals(provider) && path.contains("/models/")) {
            String[] parts = path.split("/models/");
            if (parts.length > 1) {
                return parts[1].split("/")[0];
            }
        }

        return null;
    }
}
