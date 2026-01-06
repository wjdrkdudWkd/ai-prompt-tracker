package com.galoong.aiprompttracker.tracking.collector;

import com.galoong.aiprompttracker.tracking.context.CallRecordData;
import com.galoong.aiprompttracker.tracking.context.ExecutionContext;
import com.galoong.aiprompttracker.tracking.context.TrackingContext;
import com.galoong.aiprompttracker.tracking.storage.CallStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Default implementation of CallCollector.
 * Records calls to both the ThreadLocal context and the CallStore.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultCallCollector implements CallCollector {

    private final CallStore callStore;

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
                .wasTruncated(input.getWasTruncated())
                .createdAt(Instant.now())
                .build();

        // Add to execution context (for aggregation)
        TrackingContext.addCall(callData);

        // Persist via CallStore
        callStore.save(context.getExecutionId(), input);
    }
}
