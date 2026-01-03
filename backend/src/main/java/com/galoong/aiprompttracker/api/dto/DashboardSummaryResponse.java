package com.galoong.aiprompttracker.api.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Dashboard summary statistics response
 */
@Data
@Builder
public class DashboardSummaryResponse {

    /**
     * Total cost across all executions (USD)
     */
    private Double totalCost;

    /**
     * Total number of executions
     */
    private Long totalExecutions;

    /**
     * Total number of AI API calls
     */
    private Long totalCalls;

    /**
     * Average call latency in milliseconds
     */
    private Double avgLatencyMs;

    /**
     * Average calls per execution
     */
    private Double avgCallsPerExecution;

    /**
     * Execution-level error rate (0.0 to 1.0)
     */
    private Double executionErrorRate;

    /**
     * Call-level error rate (0.0 to 1.0)
     */
    private Double callErrorRate;
}
