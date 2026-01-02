package com.galoong.aiprompttracker.tracking.aspect;

import com.galoong.aiprompttracker.core.annotation.AIPrompt;
import com.galoong.aiprompttracker.core.provider.AIProvider;
import com.galoong.aiprompttracker.core.provider.AIProviderResponse;
import com.galoong.aiprompttracker.tracking.detector.AIProviderDetector;
import com.galoong.aiprompttracker.tracking.service.TrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * @AIPrompt 어노테이션이 붙은 메서드를 자동으로 추적하는 AOP Aspect
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AIPromptAspect {

    private final AIProviderDetector providerDetector;
    private final TrackingService trackingService;

    /**
     * @AIPrompt 어노테이션이 붙은 메서드 실행 시 자동 추적
     */
    @Around("@annotation(com.galoong.aiprompttracker.core.annotation.AIPrompt)")
    public Object trackAIPrompt(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        AIPrompt annotation = method.getAnnotation(AIPrompt.class);

        if (annotation == null) {
            return joinPoint.proceed();
        }

        String functionName = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        log.debug("Tracking AI prompt function: {}", functionName);

        try {
            // 메서드 실행
            Object result = joinPoint.proceed();

            // AIProviderResponse 타입인 경우에만 추적
            if (result instanceof AIProviderResponse) {
                AIProviderResponse response = (AIProviderResponse) result;
                recordResponse(response, annotation, functionName);
            } else {
                log.debug("Return type is not AIProviderResponse, skipping tracking for: {}", functionName);
            }

            return result;

        } catch (Exception e) {
            log.error("Error executing AI prompt function: {}", functionName, e);

            // 에러 정보 기록
            recordError(annotation, functionName, e);
            throw e;
        }
    }

    private void recordResponse(AIProviderResponse response, AIPrompt annotation, String functionName) {
        try {
            trackingService.recordAICall(
                    response,
                    functionName,
                    annotation.description(),
                    annotation.category(),
                    annotation.tags()
            );
        } catch (Exception e) {
            log.error("Failed to record AI call response", e);
        }
    }

    private void recordError(AIPrompt annotation, String functionName, Exception error) {
        try {
            // Provider 찾기
            AIProvider provider = findProvider(annotation);
            if (provider == null) {
                return;
            }

            // 에러 응답 생성
            AIProviderResponse errorResponse = AIProviderResponse.builder()
                    .providerName(provider.getProviderName())
                    .modelName(annotation.model())
                    .success(false)
                    .errorMessage(error.getMessage())
                    .build();

            trackingService.recordAICall(
                    errorResponse,
                    functionName,
                    annotation.description(),
                    annotation.category(),
                    annotation.tags()
            );
        } catch (Exception e) {
            log.error("Failed to record error", e);
        }
    }

    private AIProvider findProvider(AIPrompt annotation) {
        // Provider 명시된 경우
        if (!annotation.provider().isEmpty()) {
            return providerDetector.findByName(annotation.provider()).orElse(null);
        }

        // 모델명으로 Provider 찾기
        if (!annotation.model().isEmpty()) {
            return providerDetector.findByModel(annotation.model()).orElse(null);
        }

        // 기본 Provider 사용
        return providerDetector.getDefaultProvider().orElse(null);
    }
}
