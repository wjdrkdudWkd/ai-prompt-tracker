package com.galoong.aiprompttracker.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * Execution detail with call timeline
 */
@Data
@Builder
public class ExecutionDetailResponse {

    /**
     * Execution ID
     */
    private String executionId;

    /**
     * Function name
     */
    private String functionName;

    /**
     * Category
     */
    private String category;

    /**
     * Tags
     */
    private String[] tags;

    /**
     * Environment
     */
    private String environment;

    /**
     * Started timestamp
     */
    private Instant startedAt;

    /**
     * Finished timestamp
     */
    private Instant finishedAt;

    /**
     * Duration in milliseconds
     */
    private Long durationMs;

    /**
     * Status (success/error)
     */
    private String status;

    /**
     * Error message (if any)
     */
    private String errorMessage;

    /**
     * Number of calls
     */
    private Integer callsCount;

    /**
     * Total tokens consumed
     */
    private Long totalTokens;

    /**
     * Total cost (USD)
     */
    private Double totalCost;

    /**
     * Call timeline
     */
    private List<CallDetail> calls;

    @Data
    @Builder
    public static class CallDetail {
        private String callId;
        private String provider;
        private String model;
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;
        private Double cost;
        private Long latencyMs;
        private String status;
        private String errorType;
        private String errorMessage;
        private Boolean wasTruncated;
        private String requestPreview;
        private String responsePreview;
        private Instant createdAt;
    }
}
