package com.galoong.aiprompttracker.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Execution summary for list view
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionSummaryResponse {

    /**
     * Execution ID
     */
    private String executionId;

    /**
     * Function name
     */
    private String functionName;

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
     * Status (success/error)
     */
    private String status;

    /**
     * Environment
     */
    private String environment;
}
