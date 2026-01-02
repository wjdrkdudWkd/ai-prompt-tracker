package com.galoong.aiprompttracker.core.provider;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * AI API 호출의 사용량 메트릭
 */
@Data
@Builder
public class UsageMetrics {
    /**
     * 프롬프트 토큰 수
     */
    private Integer promptTokens;

    /**
     * 완성(응답) 토큰 수
     */
    private Integer completionTokens;

    /**
     * 총 토큰 수
     */
    private Integer totalTokens;

    /**
     * 예상 비용 (USD)
     */
    private BigDecimal estimatedCost;

    /**
     * 응답 시간 (밀리초)
     */
    private Long responseTimeMs;

    /**
     * 캐시 히트 여부
     */
    private Boolean cacheHit;

    /**
     * 추가 메타데이터
     */
    private String metadata;

    /**
     * 총 토큰 수 계산
     */
    public Integer calculateTotalTokens() {
        if (totalTokens != null) {
            return totalTokens;
        }
        return (promptTokens != null ? promptTokens : 0) +
               (completionTokens != null ? completionTokens : 0);
    }
}
