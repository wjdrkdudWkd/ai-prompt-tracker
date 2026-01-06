package com.galoong.aiprompttracker.api.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Function detail with provider/model breakdown
 */
@Data
@Builder
public class FunctionDetailResponse {

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
     * Total executions
     */
    private Long totalExecutions;

    /**
     * Total calls
     */
    private Long totalCalls;

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

    /**
     * Top providers breakdown
     */
    private List<ProviderBreakdown> topProviders;

    /**
     * Top models breakdown
     */
    private List<ModelBreakdown> topModels;

    @Data
    @Builder
    public static class ProviderBreakdown {
        private String provider;
        private Long calls;
        private Double cost;
    }

    @Data
    @Builder
    public static class ModelBreakdown {
        private String model;
        private Long calls;
        private Double cost;
    }
}
