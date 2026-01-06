package com.galoong.aiprompttracker.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Call detail response for list/search
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallResponse {

    /**
     * Call ID
     */
    private String callId;

    /**
     * Execution ID
     */
    private String executionId;

    /**
     * Function name (from execution)
     */
    private String functionName;

    /**
     * Provider
     */
    private String provider;

    /**
     * Model
     */
    private String model;

    /**
     * Prompt tokens
     */
    private Integer promptTokens;

    /**
     * Completion tokens
     */
    private Integer completionTokens;

    /**
     * Total tokens
     */
    private Integer totalTokens;

    /**
     * Cost (USD)
     */
    private Double cost;

    /**
     * Latency in milliseconds
     */
    private Long latencyMs;

    /**
     * Status (success/error)
     */
    private String status;

    /**
     * Error type
     */
    private String errorType;

    /**
     * Error message
     */
    private String errorMessage;

    /**
     * Whether response was truncated
     */
    private Boolean wasTruncated;

    /**
     * Request preview
     */
    private String requestPreview;

    /**
     * Response preview
     */
    private String responsePreview;

    /**
     * Created timestamp
     */
    private Instant createdAt;
}
