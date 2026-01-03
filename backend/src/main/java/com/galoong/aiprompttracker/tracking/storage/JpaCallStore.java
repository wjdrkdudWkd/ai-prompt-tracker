package com.galoong.aiprompttracker.tracking.storage;

import com.galoong.aiprompttracker.domain.entity.CallRecord;
import com.galoong.aiprompttracker.domain.repository.CallRepository;
import com.galoong.aiprompttracker.tracking.collector.CallRecordInput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

/**
 * JPA implementation of CallStore that persists to database.
 *
 * <p>Used when JDBC persistence mode is enabled.
 */
@Slf4j
@RequiredArgsConstructor
public class JpaCallStore implements CallStore {

    private final CallRepository callRepository;

    @Override
    public void save(String executionId, CallRecordInput input) {
        try {
            CallRecord record = CallRecord.builder()
                    .executionId(executionId)
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

            callRepository.save(record);

            log.debug("Persisted call: executionId={}, provider={}, model={}",
                    executionId, input.getProvider(), input.getModel());

        } catch (Exception e) {
            log.error("Failed to persist call record", e);
        }
    }
}
