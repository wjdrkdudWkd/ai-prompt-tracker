package com.galoong.aiprompttracker.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI API 호출 기록 엔티티
 */
@Entity
@Table(name = "ai_call_records", indexes = {
        @Index(name = "idx_provider_model", columnList = "provider_name,model_name"),
        @Index(name = "idx_function_name", columnList = "function_name"),
        @Index(name = "idx_created_at", columnList = "created_at"),
        @Index(name = "idx_category", columnList = "category")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AICallRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Provider 이름 (OpenAI, Anthropic, Google)
     */
    @Column(name = "provider_name", nullable = false, length = 50)
    private String providerName;

    /**
     * 모델명
     */
    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    /**
     * 함수명 (메서드명)
     */
    @Column(name = "function_name", nullable = false, length = 200)
    private String functionName;

    /**
     * 함수 설명
     */
    @Column(name = "function_description", length = 500)
    private String functionDescription;

    /**
     * 카테고리
     */
    @Column(name = "category", length = 100)
    private String category;

    /**
     * 요청 프롬프트
     */
    @Column(name = "prompt", columnDefinition = "TEXT")
    private String prompt;

    /**
     * 응답 내용
     */
    @Column(name = "response_content", columnDefinition = "TEXT")
    private String responseContent;

    /**
     * 프롬프트 토큰 수
     */
    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    /**
     * 완성 토큰 수
     */
    @Column(name = "completion_tokens")
    private Integer completionTokens;

    /**
     * 총 토큰 수
     */
    @Column(name = "total_tokens")
    private Integer totalTokens;

    /**
     * 예상 비용 (USD)
     */
    @Column(name = "estimated_cost", precision = 10, scale = 6)
    private BigDecimal estimatedCost;

    /**
     * 응답 시간 (밀리초)
     */
    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    /**
     * 완료 사유
     */
    @Column(name = "finish_reason", length = 50)
    private String finishReason;

    /**
     * 성공 여부
     */
    @Column(name = "success")
    private Boolean success;

    /**
     * 에러 메시지
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 캐시 히트 여부
     */
    @Column(name = "cache_hit")
    private Boolean cacheHit;

    /**
     * 요청 파라미터 (JSON)
     */
    @Column(name = "parameters", columnDefinition = "TEXT")
    private String parameters;

    /**
     * 태그 (쉼표 구분)
     */
    @Column(name = "tags", length = 500)
    private String tags;

    /**
     * 생성 시간
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 사용자 ID (선택적)
     */
    @Column(name = "user_id", length = 100)
    private String userId;

    /**
     * 세션 ID (선택적)
     */
    @Column(name = "session_id", length = 100)
    private String sessionId;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (success == null) {
            success = true;
        }
        if (cacheHit == null) {
            cacheHit = false;
        }
    }
}
