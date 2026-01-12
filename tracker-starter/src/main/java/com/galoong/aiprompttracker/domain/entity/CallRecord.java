package com.galoong.aiprompttracker.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Call record entity - tracks a single AI API HTTP call.
 * Multiple calls can belong to one execution.
 *
 * <p><b>Schema Management:</b>
 * Indexes are managed by Flyway migrations (V1__create_schema_h2.sql) to avoid
 * conflicts when both Flyway and Hibernate ddl-auto are enabled.
 * When using Hibernate-only mode (flyway.enabled=false), Hibernate will create
 * the table but without indexes unless explicitly added via ddl-auto scripts.
 */
@Entity
@Table(name = "calls")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallRecord {

    /**
     * Unique call ID (auto-generated)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    /**
     * Execution ID this call belongs to
     */
    @Column(name = "execution_id", nullable = false, length = 36)
    private String executionId;

    /**
     * AI Provider name (OpenAI, Anthropic, Google, etc.)
     */
    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    /**
     * Model name (gpt-4, claude-3-opus, gemini-pro, etc.)
     */
    @Column(name = "model", nullable = false, length = 100)
    private String model;

    /**
     * Number of prompt/input tokens
     */
    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    /**
     * Number of completion/output tokens
     */
    @Column(name = "completion_tokens")
    private Integer completionTokens;

    /**
     * Total tokens
     */
    @Column(name = "total_tokens")
    private Integer totalTokens;

    /**
     * Estimated cost in USD
     */
    @Column(name = "cost")
    private Double cost;

    /**
     * API call latency in milliseconds
     */
    @Column(name = "latency_ms", nullable = false)
    private Long latencyMs;

    /**
     * Call status: "success" or "error"
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /**
     * Error type (if status is error)
     */
    @Column(name = "error_type", length = 100)
    private String errorType;

    /**
     * Error message (if status is error)
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Request preview (truncated, only in dev/test)
     */
    @Column(name = "request_preview", columnDefinition = "TEXT")
    private String requestPreview;

    /**
     * Response preview (truncated, only in dev/test)
     */
    @Column(name = "response_preview", columnDefinition = "TEXT")
    private String responsePreview;

    /**
     * Full raw JSON (only in dev/test)
     */
    @Column(name = "raw_json", columnDefinition = "TEXT")
    private String rawJson;

    /**
     * Whether the response was truncated due to size limits
     */
    @Column(name = "was_truncated")
    @Builder.Default
    private Boolean wasTruncated = false;

    /**
     * Timestamp when the call was made
     */
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
