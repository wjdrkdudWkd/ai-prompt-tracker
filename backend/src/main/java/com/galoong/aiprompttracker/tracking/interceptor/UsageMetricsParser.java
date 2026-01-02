package com.galoong.aiprompttracker.tracking.interceptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Parses usage metrics (tokens, cost) from AI provider response bodies.
 * Handles different provider response formats.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UsageMetricsParser {

    private final ObjectMapper objectMapper;

    /**
     * Parse usage metrics from response body
     */
    public ParsedUsageMetrics parse(String responseBody, String provider) {
        if (responseBody == null || responseBody.isEmpty()) {
            return ParsedUsageMetrics.empty();
        }

        try {
            JsonNode json = objectMapper.readTree(responseBody);

            switch (provider) {
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
                    log.debug("No parser for provider: {}", provider);
                    return ParsedUsageMetrics.empty();
            }
        } catch (Exception e) {
            log.warn("Failed to parse usage metrics for provider: {}", provider, e);
            return ParsedUsageMetrics.empty();
        }
    }

    private ParsedUsageMetrics parseOpenAI(JsonNode json) {
        JsonNode usage = json.get("usage");
        if (usage == null) {
            return ParsedUsageMetrics.empty();
        }

        return ParsedUsageMetrics.builder()
                .inputTokens(usage.has("prompt_tokens") ? usage.get("prompt_tokens").asInt() : null)
                .outputTokens(usage.has("completion_tokens") ? usage.get("completion_tokens").asInt() : null)
                .totalTokens(usage.has("total_tokens") ? usage.get("total_tokens").asInt() : null)
                .build();
    }

    private ParsedUsageMetrics parseAnthropic(JsonNode json) {
        JsonNode usage = json.get("usage");
        if (usage == null) {
            return ParsedUsageMetrics.empty();
        }

        return ParsedUsageMetrics.builder()
                .inputTokens(usage.has("input_tokens") ? usage.get("input_tokens").asInt() : null)
                .outputTokens(usage.has("output_tokens") ? usage.get("output_tokens").asInt() : null)
                .build();
    }

    private ParsedUsageMetrics parseGoogle(JsonNode json) {
        JsonNode metadata = json.get("usageMetadata");
        if (metadata == null) {
            return ParsedUsageMetrics.empty();
        }

        return ParsedUsageMetrics.builder()
                .inputTokens(metadata.has("promptTokenCount") ? metadata.get("promptTokenCount").asInt() : null)
                .outputTokens(metadata.has("candidatesTokenCount") ? metadata.get("candidatesTokenCount").asInt() : null)
                .totalTokens(metadata.has("totalTokenCount") ? metadata.get("totalTokenCount").asInt() : null)
                .build();
    }

    private ParsedUsageMetrics parseCohere(JsonNode json) {
        JsonNode meta = json.get("meta");
        if (meta == null || !meta.has("billed_units")) {
            return ParsedUsageMetrics.empty();
        }

        JsonNode billedUnits = meta.get("billed_units");
        return ParsedUsageMetrics.builder()
                .inputTokens(billedUnits.has("input_tokens") ? billedUnits.get("input_tokens").asInt() : null)
                .outputTokens(billedUnits.has("output_tokens") ? billedUnits.get("output_tokens").asInt() : null)
                .build();
    }

    private ParsedUsageMetrics parseMistral(JsonNode json) {
        JsonNode usage = json.get("usage");
        if (usage == null) {
            return ParsedUsageMetrics.empty();
        }

        return ParsedUsageMetrics.builder()
                .inputTokens(usage.has("prompt_tokens") ? usage.get("prompt_tokens").asInt() : null)
                .outputTokens(usage.has("completion_tokens") ? usage.get("completion_tokens").asInt() : null)
                .totalTokens(usage.has("total_tokens") ? usage.get("total_tokens").asInt() : null)
                .build();
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
