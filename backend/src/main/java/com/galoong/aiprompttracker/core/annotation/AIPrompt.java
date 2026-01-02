package com.galoong.aiprompttracker.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AI 함수 자동 추적을 위한 어노테이션
 *
 * 사용 예시:
 * <pre>
 * {@code
 * @AIPrompt(
 *     provider = "OpenAI",
 *     model = "gpt-4",
 *     description = "고객 질문에 대한 답변 생성"
 * )
 * public String answerQuestion(@PromptParam String question) {
 *     // ...
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AIPrompt {

    /**
     * Provider 이름 (OpenAI, Anthropic, Google 등)
     * 기본값: 자동 감지
     */
    String provider() default "";

    /**
     * 사용할 모델명
     * 예: "gpt-4", "claude-3-opus", "gemini-pro"
     */
    String model() default "";

    /**
     * 함수 설명 (Dashboard 표시용)
     */
    String description() default "";

    /**
     * 카테고리 (그룹핑용)
     * 예: "customer-service", "content-generation"
     */
    String category() default "default";

    /**
     * 비용 추적 활성화 여부
     */
    boolean trackCost() default true;

    /**
     * 성능 추적 활성화 여부
     */
    boolean trackPerformance() default true;

    /**
     * 캐싱 사용 여부
     */
    boolean useCache() default false;

    /**
     * 캐시 TTL (초)
     */
    int cacheTtl() default 3600;

    /**
     * 태그 (검색/필터링용)
     */
    String[] tags() default {};
}
