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
 *
 * <p><b>Schema Management:</b>
 * Indexes are managed by Flyway migrations (V1__create_schema_h2.sql) to avoid
 * conflicts when both Flyway and Hibernate ddl-auto are enabled.
 * When using Hibernate-only mode (flyway.enabled=false), Hibernate will create
 * the table but without indexes unless explicitly added via ddl-auto scripts.
 */
@Entity
@Table(name = "executions")
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
    @Column(name = "total_cost")
    private Double totalCost;

    /**
     * Total tokens across all calls
     */
    @Column(name = "total_tokens")
    private Long totalTokens;
}
