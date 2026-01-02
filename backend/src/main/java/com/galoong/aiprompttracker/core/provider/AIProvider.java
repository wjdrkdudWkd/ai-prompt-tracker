package com.galoong.aiprompttracker.core.provider;

import java.util.Map;

/**
 * AI Provider 추상 인터페이스
 * OpenAI, Anthropic, Google 등 모든 Provider가 구현해야 하는 계약
 */
public interface AIProvider {

    /**
     * Provider 이름 반환 (예: "OpenAI", "Anthropic", "Google")
     */
    String getProviderName();

    /**
     * 지원하는 모델 목록 반환
     */
    String[] getSupportedModels();

    /**
     * AI API 호출 실행
     *
     * @param modelName 사용할 모델명
     * @param prompt 프롬프트 텍스트
     * @param parameters 추가 파라미터 (temperature, max_tokens 등)
     * @return API 응답 객체
     */
    AIProviderResponse execute(String modelName, String prompt, Map<String, Object> parameters);

    /**
     * 스트리밍 방식 API 호출
     *
     * @param modelName 사용할 모델명
     * @param prompt 프롬프트 텍스트
     * @param parameters 추가 파라미터
     * @param streamCallback 스트리밍 콜백 함수
     * @return 최종 응답 객체
     */
    AIProviderResponse executeStream(String modelName, String prompt,
                                     Map<String, Object> parameters,
                                     StreamCallback streamCallback);

    /**
     * 특정 모델의 가격 정보 조회
     *
     * @param modelName 모델명
     * @return 가격 정보
     */
    PricingModel getPricing(String modelName);

    /**
     * 사용량 메트릭 계산
     *
     * @param response API 응답
     * @return 사용량 메트릭
     */
    UsageMetrics calculateUsage(AIProviderResponse response);

    /**
     * Provider 상태 확인 (Health Check)
     *
     * @return 정상 여부
     */
    boolean isHealthy();

    /**
     * 스트리밍 콜백 인터페이스
     */
    @FunctionalInterface
    interface StreamCallback {
        void onChunk(String chunk);
    }
}
