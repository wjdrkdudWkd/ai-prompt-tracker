package com.galoong.aiprompttracker.core.provider;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * AI 모델의 가격 정보
 */
@Data
@Builder
public class PricingModel {
    /**
     * 모델 이름
     */
    private String modelName;

    /**
     * 프롬프트 토큰 가격 (per 1000 tokens, USD)
     */
    private BigDecimal promptTokenPrice;

    /**
     * 완성 토큰 가격 (per 1000 tokens, USD)
     */
    private BigDecimal completionTokenPrice;

    /**
     * 최소 요금 (USD)
     */
    private BigDecimal minimumCharge;

    /**
     * 통화 코드
     */
    private String currency;

    /**
     * 비용 계산
     */
    public BigDecimal calculateCost(Integer promptTokens, Integer completionTokens) {
        BigDecimal cost = BigDecimal.ZERO;

        if (promptTokens != null && promptTokenPrice != null) {
            cost = cost.add(promptTokenPrice.multiply(BigDecimal.valueOf(promptTokens))
                    .divide(BigDecimal.valueOf(1000), 6, BigDecimal.ROUND_HALF_UP));
        }

        if (completionTokens != null && completionTokenPrice != null) {
            cost = cost.add(completionTokenPrice.multiply(BigDecimal.valueOf(completionTokens))
                    .divide(BigDecimal.valueOf(1000), 6, BigDecimal.ROUND_HALF_UP));
        }

        // 최소 요금 적용
        if (minimumCharge != null && cost.compareTo(minimumCharge) < 0) {
            cost = minimumCharge;
        }

        return cost;
    }
}
