package com.galoong.aiprompttracker.tracking.context;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Data transfer object for a single AI API call
 */
@Data
@Builder
public class CallRecordData {

    /**
     * AI Provider name (OpenAI, Anthropic, Google, etc.)
     */
    private String provider;

    /**
     * Model name (gpt-4, claude-3-opus, gemini-pro, etc.)
     */
    private String model;

    /**
     * Number of prompt/input tokens
     */
    private Integer promptTokens;

    /**
     * Number of completion/output tokens
     */
    private Integer completionTokens;

    /**
     * Total tokens (may be calculated or from API response)
     */
    private Integer totalTokens;

    /**
     * Estimated cost in USD
     */
    private Double cost;

    /**
     * API call latency in milliseconds
     */
    private Long latencyMs;

    /**
     * Call status: "success" or "error"
     */
    private String status;

    /**
     * Error type (if status is error)
     */
    private String errorType;

    /**
     * Error message (if status is error)
     */
    private String errorMessage;

    /**
     * Request preview (truncated, only in dev/test)
     */
    private String requestPreview;

    /**
     * Response preview (truncated, only in dev/test)
     */
    private String responsePreview;

    /**
     * Full raw JSON (only in dev/test)
     */
    private String rawJson;

    /**
     * Timestamp when the call was made
     */
    @Builder.Default
    private Instant createdAt = Instant.now();
}
