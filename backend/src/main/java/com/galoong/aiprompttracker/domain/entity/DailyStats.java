package com.galoong.aiprompttracker.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 일별 통계 집계 엔티티
 */
@Entity
@Table(name = "daily_stats", indexes = {
        @Index(name = "idx_stats_date", columnList = "stats_date"),
        @Index(name = "idx_provider_date", columnList = "provider_name,stats_date"),
        @Index(name = "idx_function_date", columnList = "function_name,stats_date")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_provider_model_function_date",
                columnNames = {"provider_name", "model_name", "function_name", "stats_date"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 통계 날짜
     */
    @Column(name = "stats_date", nullable = false)
    private LocalDate statsDate;

    /**
     * Provider 이름
     */
    @Column(name = "provider_name", nullable = false, length = 50)
    private String providerName;

    /**
     * 모델명
     */
    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    /**
     * 함수명
     */
    @Column(name = "function_name", nullable = false, length = 200)
    private String functionName;

    /**
     * 카테고리
     */
    @Column(name = "category", length = 100)
    private String category;

    /**
     * 총 호출 횟수
     */
    @Column(name = "total_calls")
    private Integer totalCalls;

    /**
     * 성공 호출 횟수
     */
    @Column(name = "success_calls")
    private Integer successCalls;

    /**
     * 실패 호출 횟수
     */
    @Column(name = "failed_calls")
    private Integer failedCalls;

    /**
     * 총 프롬프트 토큰
     */
    @Column(name = "total_prompt_tokens")
    private Long totalPromptTokens;

    /**
     * 총 완성 토큰
     */
    @Column(name = "total_completion_tokens")
    private Long totalCompletionTokens;

    /**
     * 총 토큰
     */
    @Column(name = "total_tokens")
    private Long totalTokens;

    /**
     * 총 비용 (USD)
     */
    @Column(name = "total_cost", precision = 10, scale = 6)
    private BigDecimal totalCost;

    /**
     * 평균 응답 시간 (밀리초)
     */
    @Column(name = "avg_response_time_ms")
    private Long avgResponseTimeMs;

    /**
     * 최소 응답 시간 (밀리초)
     */
    @Column(name = "min_response_time_ms")
    private Long minResponseTimeMs;

    /**
     * 최대 응답 시간 (밀리초)
     */
    @Column(name = "max_response_time_ms")
    private Long maxResponseTimeMs;

    /**
     * 캐시 히트 횟수
     */
    @Column(name = "cache_hits")
    private Integer cacheHits;

    /**
     * 생성 시간
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 수정 시간
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
