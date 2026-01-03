package com.galoong.aiprompttracker.tracking.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UsageMetricsParser
 */
class UsageMetricsParserTest {

    private UsageMetricsParser parser;

    @BeforeEach
    void setUp() {
        parser = new UsageMetricsParser(new ObjectMapper());
    }

    // ===== Provider Normalization Tests =====

    @Test
    void testOpenAI_providerAliases() {
        String response = "{\"usage\":{\"prompt_tokens\":100,\"completion_tokens\":50,\"total_tokens\":150}}";

        // Test various OpenAI aliases
        assertTokens(parser.parse(response, "OpenAI"), 100, 50, 150);
        assertTokens(parser.parse(response, "openai"), 100, 50, 150);
        assertTokens(parser.parse(response, "AzureOpenAI"), 100, 50, 150);
        assertTokens(parser.parse(response, "azure-openai"), 100, 50, 150);
        assertTokens(parser.parse(response, "azure_openai"), 100, 50, 150);
    }

    @Test
    void testAnthropic_providerAliases() {
        String response = "{\"usage\":{\"input_tokens\":100,\"output_tokens\":50}}";

        assertTokens(parser.parse(response, "Anthropic"), 100, 50, 150);
        assertTokens(parser.parse(response, "anthropic"), 100, 50, 150);
        assertTokens(parser.parse(response, "claude"), 100, 50, 150);
    }

    @Test
    void testGoogle_providerAliases() {
        String response = "{\"usageMetadata\":{\"promptTokenCount\":100,\"candidatesTokenCount\":50,\"totalTokenCount\":150}}";

        assertTokens(parser.parse(response, "Google"), 100, 50, 150);
        assertTokens(parser.parse(response, "google"), 100, 50, 150);
        assertTokens(parser.parse(response, "Gemini"), 100, 50, 150);
        assertTokens(parser.parse(response, "vertex"), 100, 50, 150);
        assertTokens(parser.parse(response, "VertexAI"), 100, 50, 150);
        assertTokens(parser.parse(response, "vertex-ai"), 100, 50, 150);
    }

    @Test
    void testMistral_providerAliases() {
        String response = "{\"usage\":{\"prompt_tokens\":100,\"completion_tokens\":50,\"total_tokens\":150}}";

        assertTokens(parser.parse(response, "Mistral"), 100, 50, 150);
        assertTokens(parser.parse(response, "mistral"), 100, 50, 150);
        assertTokens(parser.parse(response, "MistralAI"), 100, 50, 150);
        assertTokens(parser.parse(response, "mistral-ai"), 100, 50, 150);
    }

    // ===== Fallback Path Tests =====

    @Test
    void testOpenAI_fallbackToInputOutputTokens() {
        // OpenAI Responses API uses input_tokens/output_tokens instead of prompt_tokens/completion_tokens
        String response = "{\"usage\":{\"input_tokens\":200,\"output_tokens\":100,\"total_tokens\":300}}";

        UsageMetricsParser.ParsedUsageMetrics result = parser.parse(response, "OpenAI");
        assertTokens(result, 200, 100, 300);
    }

    @Test
    void testOpenAI_primaryPathTakesPrecedence() {
        // If both paths exist, primary should win
        String response = "{\"usage\":{\"prompt_tokens\":100,\"completion_tokens\":50,\"input_tokens\":999,\"output_tokens\":999,\"total_tokens\":150}}";

        UsageMetricsParser.ParsedUsageMetrics result = parser.parse(response, "OpenAI");
        assertTokens(result, 100, 50, 150); // Should use prompt_tokens, not input_tokens
    }

    @Test
    void testGoogle_snakeCaseFallback() {
        // Google may use snake_case in some API variants
        String response = "{\"usage_metadata\":{\"prompt_token_count\":100,\"candidates_token_count\":50,\"total_token_count\":150}}";

        UsageMetricsParser.ParsedUsageMetrics result = parser.parse(response, "Google");
        assertTokens(result, 100, 50, 150);
    }

    @Test
    void testGoogle_camelCasePrimary() {
        String response = "{\"usageMetadata\":{\"promptTokenCount\":100,\"candidatesTokenCount\":50,\"totalTokenCount\":150}}";

        UsageMetricsParser.ParsedUsageMetrics result = parser.parse(response, "Google");
        assertTokens(result, 100, 50, 150);
    }

    // ===== Truncation Safety Tests =====

    @Test
    void testNonJsonBody_noWarning() {
        // Should return empty without WARN (only DEBUG)
        UsageMetricsParser.ParsedUsageMetrics result = parser.parse("This is plain text", "OpenAI");
        assertEmpty(result);
    }

    @Test
    void testEmptyBody() {
        assertEmpty(parser.parse("", "OpenAI"));
        assertEmpty(parser.parse(null, "OpenAI"));
    }

    @Test
    void testTruncatedJson_gracefulFailure() {
        // Truncated JSON should fail gracefully without crash
        String truncated = "{\"usage\":{\"prompt_tokens\":100,\"completion_tok";

        UsageMetricsParser.ParsedUsageMetrics result = parser.parse(truncated, "OpenAI");
        assertEmpty(result);
    }

    @Test
    void testInvalidJson_gracefulFailure() {
        String invalid = "{invalid json}";

        UsageMetricsParser.ParsedUsageMetrics result = parser.parse(invalid, "OpenAI");
        assertEmpty(result);
    }

    // ===== Standard Format Tests =====

    @Test
    void testOpenAI_standardFormat() {
        String response = "{\"usage\":{\"prompt_tokens\":500,\"completion_tokens\":300,\"total_tokens\":800}}";

        UsageMetricsParser.ParsedUsageMetrics result = parser.parse(response, "OpenAI");
        assertTokens(result, 500, 300, 800);
    }

    @Test
    void testAnthropic_standardFormat() {
        String response = "{\"usage\":{\"input_tokens\":500,\"output_tokens\":300}}";

        UsageMetricsParser.ParsedUsageMetrics result = parser.parse(response, "Anthropic");
        // Anthropic doesn't provide total_tokens, but constructor computes it
        assertTokens(result, 500, 300, 800);
    }

    @Test
    void testCohere_standardFormat() {
        String response = "{\"meta\":{\"billed_units\":{\"input_tokens\":100,\"output_tokens\":50}}}";

        UsageMetricsParser.ParsedUsageMetrics result = parser.parse(response, "Cohere");
        assertTokens(result, 100, 50, 150); // Total computed
    }

    @Test
    void testMistral_standardFormat() {
        String response = "{\"usage\":{\"prompt_tokens\":400,\"completion_tokens\":200,\"total_tokens\":600}}";

        UsageMetricsParser.ParsedUsageMetrics result = parser.parse(response, "Mistral");
        assertTokens(result, 400, 200, 600);
    }

    // ===== Missing Usage Tests =====

    @Test
    void testMissingUsageBlock() {
        String response = "{\"choices\":[{\"text\":\"Hello\"}]}"; // No usage block

        assertEmpty(parser.parse(response, "OpenAI"));
        assertEmpty(parser.parse(response, "Anthropic"));
    }

    @Test
    void testEmptyUsageBlock() {
        String response = "{\"usage\":{}}";

        UsageMetricsParser.ParsedUsageMetrics result = parser.parse(response, "OpenAI");
        assertEmpty(result);
    }

    @Test
    void testNullUsageBlock() {
        String response = "{\"usage\":null}";

        UsageMetricsParser.ParsedUsageMetrics result = parser.parse(response, "OpenAI");
        assertEmpty(result);
    }

    // ===== Unknown Provider Tests =====

    @Test
    void testUnknownProvider() {
        String response = "{\"usage\":{\"tokens\":100}}";

        UsageMetricsParser.ParsedUsageMetrics result = parser.parse(response, "UnknownProvider");
        assertEmpty(result);
    }

    @Test
    void testNullProvider() {
        String response = "{\"usage\":{\"prompt_tokens\":100}}";

        UsageMetricsParser.ParsedUsageMetrics result = parser.parse(response, null);
        assertEmpty(result);
    }

    // ===== Helper Methods =====

    private void assertTokens(UsageMetricsParser.ParsedUsageMetrics metrics, Integer expectedInput, Integer expectedOutput, Integer expectedTotal) {
        assertNotNull(metrics);
        assertEquals(expectedInput, metrics.getInputTokens(), "Input tokens mismatch");
        assertEquals(expectedOutput, metrics.getOutputTokens(), "Output tokens mismatch");
        assertEquals(expectedTotal, metrics.getTotalTokens(), "Total tokens mismatch");
    }

    private void assertEmpty(UsageMetricsParser.ParsedUsageMetrics metrics) {
        assertNotNull(metrics);
        assertNull(metrics.getInputTokens());
        assertNull(metrics.getOutputTokens());
        assertNull(metrics.getTotalTokens());
    }
}
