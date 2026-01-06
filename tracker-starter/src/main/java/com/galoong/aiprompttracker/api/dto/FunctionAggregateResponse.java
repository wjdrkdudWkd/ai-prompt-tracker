package com.galoong.aiprompttracker.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Function-level aggregate statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunctionAggregateResponse {

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
     * Total number of executions
     */
    private Long executions;

    /**
     * Total number of calls
     */
    private Long calls;

    /**
     * Average calls per execution
     */
    private Double callsPerExecution;

    /**
     * Total cost (USD)
     */
    private Double totalCost;

    /**
     * Average execution time in milliseconds
     */
    private Double avgExecutionTimeMs;

    /**
     * Error rate (0.0 to 1.0)
     */
    private Double errorRate;
}
