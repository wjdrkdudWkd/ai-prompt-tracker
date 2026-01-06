package com.galoong.aiprompttracker.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AI 프롬프트 파라미터 표시 어노테이션
 *
 * 사용 예시:
 * <pre>
 * {@code
 * @AIPrompt(provider = "OpenAI", model = "gpt-4")
 * public String translate(
 *     @PromptParam(name = "text", description = "번역할 텍스트") String text,
 *     @PromptParam(name = "targetLang", description = "목표 언어") String targetLang
 * ) {
 *     // ...
 * }
 * }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface PromptParam {

    /**
     * 파라미터 이름
     */
    String name() default "";

    /**
     * 파라미터 설명
     */
    String description() default "";

    /**
     * 필수 여부
     */
    boolean required() default true;

    /**
     * 기본값
     */
    String defaultValue() default "";

    /**
     * 예시 값
     */
    String example() default "";
}
