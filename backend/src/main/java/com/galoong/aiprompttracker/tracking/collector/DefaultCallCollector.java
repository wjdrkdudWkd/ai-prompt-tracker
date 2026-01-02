package com.galoong.aiprompttracker.tracking.collector;

import com.galoong.aiprompttracker.domain.entity.CallRecord;
import com.galoong.aiprompttracker.domain.repository.CallRepository;
import com.galoong.aiprompttracker.tracking.context.CallRecordData;
import com.galoong.aiprompttracker.tracking.context.ExecutionContext;
import com.galoong.aiprompttracker.tracking.context.TrackingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Default implementation of CallCollector.
 * Records calls to both the ThreadLocal context and the database.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultCallCollector implements CallCollector {

    private final CallRepository callRepository;

    @Override
    public void recordCall(CallRecordInput input) {
        if (!TrackingContext.isTracking()) {
            log.debug("Not tracking execution, skipping call record");
            return;
        }

        ExecutionContext context = TrackingContext.getCurrentExecution();

        // Convert input to CallRecordData
        CallRecordData callData = CallRecordData.builder()
                .provider(input.getProvider())
                .model(input.getModel())
                .promptTokens(input.getPromptTokens())
                .completionTokens(input.getCompletionTokens())
                .totalTokens(input.getTotalTokens())
                .cost(input.getCost())
                .latencyMs(input.getLatencyMs())
                .status(input.getStatus())
                .errorType(input.getErrorType())
                .errorMessage(input.getErrorMessage())
                .requestPreview(input.getRequestPreview())
                .responsePreview(input.getResponsePreview())
                .rawJson(input.getRawJson())
                .createdAt(Instant.now())
                .build();

        // Add to execution context (for aggregation)
        TrackingContext.addCall(callData);

        // Persist to database immediately
        try {
            CallRecord record = CallRecord.builder()
                    .executionId(context.getExecutionId())
                    .provider(input.getProvider())
                    .model(input.getModel())
                    .promptTokens(input.getPromptTokens())
                    .completionTokens(input.getCompletionTokens())
                    .totalTokens(input.getTotalTokens())
                    .cost(input.getCost())
                    .latencyMs(input.getLatencyMs())
                    .status(input.getStatus())
                    .errorType(input.getErrorType())
                    .errorMessage(input.getErrorMessage())
                    .requestPreview(input.getRequestPreview())
                    .responsePreview(input.getResponsePreview())
                    .rawJson(input.getRawJson())
                    .createdAt(Instant.now())
                    .build();

            callRepository.save(record);
            log.debug("Recorded call: executionId={}, provider={}, model={}",
                    context.getExecutionId(), input.getProvider(), input.getModel());
        } catch (Exception e) {
            log.error("Failed to persist call record", e);
        }
    }
}
