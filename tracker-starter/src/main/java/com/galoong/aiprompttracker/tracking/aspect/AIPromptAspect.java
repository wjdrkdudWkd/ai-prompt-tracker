package com.galoong.aiprompttracker.tracking.aspect;

import com.galoong.aiprompttracker.core.annotation.AIPrompt;
import com.galoong.aiprompttracker.tracking.context.ExecutionContext;
import com.galoong.aiprompttracker.tracking.context.TrackingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;

/**
 * AOP Aspect that tracks @AIPrompt annotated method executions.
 *
 * This creates an Execution context for each method invocation,
 * regardless of return type. The actual AI calls are tracked
 * by interceptors (WebClient, RestTemplate, etc.).
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AIPromptAspect {

    @Value("${spring.profiles.active:dev}")
    private String environment;

    /**
     * Track @AIPrompt annotated method execution
     */
    @Around("@annotation(aiPrompt)")
    public Object trackExecution(ProceedingJoinPoint pjp, AIPrompt aiPrompt) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();

        // Generate unique execution ID
        String executionId = UUID.randomUUID().toString();

        // Determine function name
        String functionName = aiPrompt.name();
        if (functionName == null || functionName.isEmpty()) {
            functionName = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        }

        // Create execution context
        ExecutionContext context = ExecutionContext.builder()
                .executionId(executionId)
                .functionName(functionName)
                .category(aiPrompt.category())
                .tags(aiPrompt.tags())
                .environment(environment)
                .startTime(Instant.now())
                .build();

        // Start tracking this execution
        TrackingContext.startExecution(context);

        log.debug("Started tracking execution: id={}, function={}", executionId, functionName);

        try {
            // Execute the user's method (return type doesn't matter!)
            Object result = pjp.proceed();

            // Mark execution as successful
            TrackingContext.endExecutionSuccess();

            log.info("Execution completed successfully: id={}, function={}, calls={}, cost={}",
                    executionId,
                    functionName,
                    context.getCallsCount(),
                    context.getTotalCost());

            return result;

        } catch (Throwable ex) {
            // Mark execution as failed
            TrackingContext.endExecutionError(ex);

            log.error("Execution failed: id={}, function={}, error={}",
                    executionId, functionName, ex.getMessage());

            throw ex;

        } finally {
            // Always clean up ThreadLocal
            TrackingContext.clear();
        }
    }
}
