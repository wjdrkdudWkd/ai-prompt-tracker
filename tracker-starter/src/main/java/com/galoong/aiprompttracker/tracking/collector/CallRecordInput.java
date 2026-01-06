package com.galoong.aiprompttracker.tracking.collector;

import lombok.Builder;
import lombok.Data;

/**
 * Input DTO for recording an AI API call.
 * Used by interceptors to report call metadata.
 */
@Data
@Builder
public class CallRecordInput {

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
     * Total tokens
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
     * Whether the response was truncated due to size limits
     */
    private Boolean wasTruncated;
}
