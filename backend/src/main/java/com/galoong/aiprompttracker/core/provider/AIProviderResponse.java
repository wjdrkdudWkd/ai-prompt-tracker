package com.galoong.aiprompttracker.core.provider;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * AI Provider의 표준화된 응답 형식
 */
@Data
@Builder
public class AIProviderResponse {
    /**
     * Provider 이름
     */
    private String providerName;

    /**
     * 사용된 모델명
     */
    private String modelName;

    /**
     * 응답 텍스트
     */
    private String content;

    /**
     * 요청 프롬프트
     */
    private String prompt;

    /**
     * 사용량 정보
     */
    private UsageMetrics usage;

    /**
     * 완료 사유 (stop, length, etc.)
     */
    private String finishReason;

    /**
     * 요청 시작 시간
     */
    private LocalDateTime requestTime;

    /**
     * 응답 완료 시간
     */
    private LocalDateTime responseTime;

    /**
     * 성공 여부
     */
    private Boolean success;

    /**
     * 에러 메시지 (실패 시)
     */
    private String errorMessage;

    /**
     * 원본 응답 (디버깅용)
     */
    private Map<String, Object> rawResponse;

    /**
     * 요청 파라미터
     */
    private Map<String, Object> parameters;
}
