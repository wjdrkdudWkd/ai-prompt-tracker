package com.galoong.aiprompttracker.tracking.interceptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Parses usage metrics (tokens, cost) from AI provider response bodies.
 * Handles different provider response formats with fallback paths and normalization.
 *
 * <p>Production-safe features:
 * <ul>
 *   <li>Provider alias normalization (openai, azure-openai, claude, gemini, etc.)</li>
 *   <li>Fallback parsing paths for API variants</li>
 *   <li>Graceful handling of truncated/non-JSON responses</li>
 *   <li>Reduced log noise for clearly invalid bodies</li>
 * </ul>
 */
@Slf4j
// @Component removed - registered as bean in TrackingCoreAutoConfiguration
@RequiredArgsConstructor
public class UsageMetricsParser {

    private final ObjectMapper objectMapper;

    /**
     * Parse usage metrics from response body
     *
     * @param responseBody Raw response body (may be truncated or non-JSON)
     * @param provider Provider name (normalized internally)
     * @return Parsed metrics or empty if parsing fails
     */
    public ParsedUsageMetrics parse(String responseBody, String provider) {
        if (responseBody == null || responseBody.isEmpty()) {
            return ParsedUsageMetrics.empty();
        }

        // Quick heuristic: skip obviously non-JSON bodies without WARN noise
        String trimmed = responseBody.trim();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            log.debug("Response body doesn't look like JSON (length: {}), skipping parse", trimmed.length());
            return ParsedUsageMetrics.empty();
        }

        try {
            JsonNode json = objectMapper.readTree(responseBody);
            String normalizedProvider = normalizeProvider(provider);

            switch (normalizedProvider) {
                case "OpenAI":
                    return parseOpenAI(json);
                case "Anthropic":
                    return parseAnthropic(json);
                case "Google":
                    return parseGoogle(json);
                case "Cohere":
                    return parseCohere(json);
                case "Mistral":
                    return parseMistral(json);
                default:
                    log.debug("No parser for provider: {} (normalized: {})", provider, normalizedProvider);
                    return ParsedUsageMetrics.empty();
            }
        } catch (Exception e) {
            // Reduced noise: only log provider and body length, not full content
            log.warn("Failed to parse usage metrics for provider: {} (body length: {}, error: {})",
                    provider, responseBody.length(), e.getMessage());
            log.debug("Parse error details", e); // Full stack trace only at DEBUG
            return ParsedUsageMetrics.empty();
        }
    }

    /**
     * Normalize provider name and handle aliases
     */
    private String normalizeProvider(String provider) {
        if (provider == null) {
            return "Unknown";
        }

        String normalized = provider.trim().toLowerCase();

        // OpenAI variants
        if (normalized.equals("openai") ||
            normalized.equals("azureopenai") ||
            normalized.equals("azure-openai") ||
            normalized.equals("azure_openai")) {
            return "OpenAI";
        }

        // Anthropic variants
        if (normalized.equals("anthropic") ||
            normalized.equals("claude")) {
            return "Anthropic";
        }

        // Google variants
        if (normalized.equals("google") ||
            normalized.equals("gemini") ||
            normalized.equals("vertex") ||
            normalized.equals("vertexai") ||
            normalized.equals("vertex-ai")) {
            return "Google";
        }

        // Cohere
        if (normalized.equals("cohere")) {
            return "Cohere";
        }

        // Mistral
        if (normalized.equals("mistral") ||
            normalized.equals("mistralai") ||
            normalized.equals("mistral-ai")) {
            return "Mistral";
        }

        // Return original if no match
        return provider;
    }

    /**
     * Parse OpenAI format with fallback paths
     *
     * <p>Primary: usage.prompt_tokens, usage.completion_tokens, usage.total_tokens
     * <p>Fallback: usage.input_tokens, usage.output_tokens (Responses API variant)
     */
    private ParsedUsageMetrics parseOpenAI(JsonNode json) {
        JsonNode usage = getNode(json, "usage");
        if (usage == null || usage.isNull()) {
            return ParsedUsageMetrics.empty();
        }

        // Try primary paths (Chat Completions API)
        Integer inputTokens = getInt(usage, "prompt_tokens");
        Integer outputTokens = getInt(usage, "completion_tokens");
        Integer totalTokens = getInt(usage, "total_tokens");

        // Fallback to alternative paths (Responses API, Assistants API)
        if (inputTokens == null) {
            inputTokens = getInt(usage, "input_tokens");
        }
        if (outputTokens == null) {
            outputTokens = getInt(usage, "output_tokens");
        }

        return ParsedUsageMetrics.builder()
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .totalTokens(totalTokens)
                .build();
    }

    /**
     * Parse Anthropic format
     *
     * <p>Primary: usage.input_tokens, usage.output_tokens
     */
    private ParsedUsageMetrics parseAnthropic(JsonNode json) {
        JsonNode usage = getNode(json, "usage");
        if (usage == null || usage.isNull()) {
            return ParsedUsageMetrics.empty();
        }

        return ParsedUsageMetrics.builder()
                .inputTokens(getInt(usage, "input_tokens"))
                .outputTokens(getInt(usage, "output_tokens"))
                .build();
    }

    /**
     * Parse Google (Gemini/Vertex) format with fallback paths
     *
     * <p>Primary: usageMetadata.promptTokenCount, usageMetadata.candidatesTokenCount, usageMetadata.totalTokenCount
     * <p>Fallback: usage_metadata (snake_case variant)
     */
    private ParsedUsageMetrics parseGoogle(JsonNode json) {
        // Try camelCase (standard)
        JsonNode metadata = getNode(json, "usageMetadata");

        // Fallback to snake_case
        if (metadata == null || metadata.isNull()) {
            metadata = getNode(json, "usage_metadata");
        }

        if (metadata == null || metadata.isNull()) {
            return ParsedUsageMetrics.empty();
        }

        // Try camelCase fields
        Integer inputTokens = getInt(metadata, "promptTokenCount");
        Integer outputTokens = getInt(metadata, "candidatesTokenCount");
        Integer totalTokens = getInt(metadata, "totalTokenCount");

        // Fallback to snake_case fields
        if (inputTokens == null) {
            inputTokens = getInt(metadata, "prompt_token_count");
        }
        if (outputTokens == null) {
            outputTokens = getInt(metadata, "candidates_token_count");
        }
        if (totalTokens == null) {
            totalTokens = getInt(metadata, "total_token_count");
        }

        return ParsedUsageMetrics.builder()
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .totalTokens(totalTokens)
                .build();
    }

    /**
     * Parse Cohere format
     *
     * <p>Primary: meta.billed_units.input_tokens, meta.billed_units.output_tokens
     */
    private ParsedUsageMetrics parseCohere(JsonNode json) {
        JsonNode meta = getNode(json, "meta");
        if (meta == null || meta.isNull()) {
            return ParsedUsageMetrics.empty();
        }

        JsonNode billedUnits = getNode(meta, "billed_units");
        if (billedUnits == null || billedUnits.isNull()) {
            return ParsedUsageMetrics.empty();
        }

        return ParsedUsageMetrics.builder()
                .inputTokens(getInt(billedUnits, "input_tokens"))
                .outputTokens(getInt(billedUnits, "output_tokens"))
                .build();
    }

    /**
     * Parse Mistral format (same as OpenAI Chat Completions)
     *
     * <p>Primary: usage.prompt_tokens, usage.completion_tokens, usage.total_tokens
     */
    private ParsedUsageMetrics parseMistral(JsonNode json) {
        JsonNode usage = getNode(json, "usage");
        if (usage == null || usage.isNull()) {
            return ParsedUsageMetrics.empty();
        }

        return ParsedUsageMetrics.builder()
                .inputTokens(getInt(usage, "prompt_tokens"))
                .outputTokens(getInt(usage, "completion_tokens"))
                .totalTokens(getInt(usage, "total_tokens"))
                .build();
    }

    // ===== Helper Methods =====

    /**
     * Safely get a child node
     */
    private JsonNode getNode(JsonNode parent, String fieldName) {
        if (parent == null || parent.isNull() || !parent.has(fieldName)) {
            return null;
        }
        JsonNode child = parent.get(fieldName);
        return (child != null && !child.isNull()) ? child : null;
    }

    /**
     * Safely get an integer value from a field
     */
    private Integer getInt(JsonNode node, String fieldName) {
        if (node == null || node.isNull() || !node.has(fieldName)) {
            return null;
        }
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull() || !field.isNumber()) {
            return null;
        }
        return field.asInt();
    }

    /**
     * Parsed usage metrics
     */
    public static class ParsedUsageMetrics {
        private final Integer inputTokens;
        private final Integer outputTokens;
        private final Integer totalTokens;

        private ParsedUsageMetrics(Integer inputTokens, Integer outputTokens, Integer totalTokens) {
            this.inputTokens = inputTokens;
            this.outputTokens = outputTokens;
            // Auto-compute total if missing but both input/output present
            this.totalTokens = totalTokens != null ? totalTokens :
                    (inputTokens != null && outputTokens != null ? inputTokens + outputTokens : null);
        }

        public static Builder builder() {
            return new Builder();
        }

        public static ParsedUsageMetrics empty() {
            return new ParsedUsageMetrics(null, null, null);
        }

        public Integer getInputTokens() {
            return inputTokens;
        }

        public Integer getOutputTokens() {
            return outputTokens;
        }

        public Integer getTotalTokens() {
            return totalTokens;
        }

        public static class Builder {
            private Integer inputTokens;
            private Integer outputTokens;
            private Integer totalTokens;

            public Builder inputTokens(Integer inputTokens) {
                this.inputTokens = inputTokens;
                return this;
            }

            public Builder outputTokens(Integer outputTokens) {
                this.outputTokens = outputTokens;
                return this;
            }

            public Builder totalTokens(Integer totalTokens) {
                this.totalTokens = totalTokens;
                return this;
            }

            public ParsedUsageMetrics build() {
                return new ParsedUsageMetrics(inputTokens, outputTokens, totalTokens);
            }
        }
    }
}
