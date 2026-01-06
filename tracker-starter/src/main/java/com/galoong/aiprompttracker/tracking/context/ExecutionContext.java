package com.galoong.aiprompttracker.tracking.context;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Execution context that tracks a single @AIPrompt annotated method execution
 * and all AI API calls made within it.
 */
@Data
@Builder
public class ExecutionContext {

    /**
     * Unique execution ID (UUID)
     */
    private String executionId;

    /**
     * Function name from @AIPrompt annotation
     */
    private String functionName;

    /**
     * Category from @AIPrompt annotation
     */
    private String category;

    /**
     * Tags from @AIPrompt annotation
     */
    private String[] tags;

    /**
     * Environment (dev, test, prod)
     */
    private String environment;

    /**
     * Execution start time
     */
    private Instant startTime;

    /**
     * Execution end time
     */
    private Instant endTime;

    /**
     * Status: "success" or "error"
     */
    private String status;

    /**
     * Error message if status is "error"
     */
    private String errorMessage;

    /**
     * Total number of AI calls made during this execution
     */
    @Builder.Default
    private int callsCount = 0;

    /**
     * Total cost across all calls (in USD)
     */
    private Double totalCost;

    /**
     * Total tokens across all calls
     */
    private Long totalTokens;

    /**
     * All AI calls made during this execution
     */
    @Builder.Default
    private List<CallRecordData> calls = new ArrayList<>();

    /**
     * Add a call record to this execution
     */
    public void addCall(CallRecordData call) {
        calls.add(call);
        callsCount++;

        if (call.getCost() != null) {
            totalCost = (totalCost == null ? 0.0 : totalCost) + call.getCost();
        }

        if (call.getTotalTokens() != null) {
            totalTokens = (totalTokens == null ? 0L : totalTokens) + call.getTotalTokens();
        }
    }

    /**
     * Mark execution as successful
     */
    public void markSuccess() {
        this.endTime = Instant.now();
        this.status = "success";
    }

    /**
     * Mark execution as failed
     */
    public void markError(String message) {
        this.endTime = Instant.now();
        this.status = "error";
        this.errorMessage = message;
    }

    /**
     * Calculate execution duration in milliseconds
     */
    public Long getDurationMs() {
        if (startTime != null && endTime != null) {
            return endTime.toEpochMilli() - startTime.toEpochMilli();
        }
        return null;
    }
}
