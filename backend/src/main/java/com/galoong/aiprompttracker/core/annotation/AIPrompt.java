package com.galoong.aiprompttracker.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AI Prompt execution tracking annotation
 *
 * Automatically tracks method executions and AI API calls made within them.
 * Works with any return type - does not force AIProviderResponse.
 *
 * Usage:
 * <pre>
 * {@code
 * @AIPrompt(
 *     name = "extractWords",
 *     description = "Extract Japanese words from subtitle",
 *     category = "nlp"
 * )
 * public MyResult analyze(String subtitle) {
 *     // Your code calls AI APIs using WebClient, RestTemplate, etc.
 *     // All calls are automatically tracked!
 *     return result;
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AIPrompt {

    /**
     * Function name (for identification in dashboard)
     * If not specified, uses ClassName.methodName
     */
    String name() default "";

    /**
     * Function description (for dashboard display)
     */
    String description() default "";

    /**
     * Category (for grouping functions)
     * Example: "nlp", "content-generation", "customer-service"
     */
    String category() default "default";

    /**
     * Tags (for search and filtering)
     * Example: {"subtitle", "japanese", "wordextraction"}
     */
    String[] tags() default {};
}
