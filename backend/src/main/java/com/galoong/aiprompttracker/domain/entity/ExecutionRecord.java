package com.galoong.aiprompttracker.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Execution record entity - tracks a single @AIPrompt method execution.
 * One execution can contain multiple AI API calls.
 */
@Entity
@Table(name = "executions", indexes = {
        @Index(name = "idx_executions_function", columnList = "function_name"),
        @Index(name = "idx_executions_started_at", columnList = "started_at"),
        @Index(name = "idx_executions_environment", columnList = "environment"),
        @Index(name = "idx_executions_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionRecord {

    /**
     * Unique execution ID (UUID)
     */
    @Id
    @Column(length = 36)
    private String id;

    /**
     * Function name from @AIPrompt annotation
     */
    @Column(name = "function_name", nullable = false, length = 100)
    private String functionName;

    /**
     * Category from @AIPrompt annotation
     */
    @Column(name = "category", length = 50)
    private String category;

    /**
     * Tags from @AIPrompt annotation (stored as array)
     */
    @Column(name = "tags", columnDefinition = "TEXT[]")
    private String[] tags;

    /**
     * Environment (dev, test, prod, advanced)
     */
    @Column(name = "environment", nullable = false, length = 20)
    private String environment;

    /**
     * Execution start time
     */
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    /**
     * Execution finish time
     */
    @Column(name = "finished_at")
    private Instant finishedAt;

    /**
     * Execution duration in milliseconds
     */
    @Column(name = "duration_ms")
    private Long durationMs;

    /**
     * Status: "success" or "error"
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /**
     * Error message (if status is error)
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Total number of AI calls in this execution
     */
    @Column(name = "calls_count")
    @Builder.Default
    private Integer callsCount = 0;

    /**
     * Total cost across all calls (USD)
     */
    @Column(name = "total_cost", precision = 10, scale = 6)
    private Double totalCost;

    /**
     * Total tokens across all calls
     */
    @Column(name = "total_tokens")
    private Long totalTokens;
}
