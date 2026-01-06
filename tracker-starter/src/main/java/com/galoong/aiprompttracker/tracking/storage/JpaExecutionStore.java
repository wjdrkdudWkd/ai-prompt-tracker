package com.galoong.aiprompttracker.tracking.storage;

import com.galoong.aiprompttracker.domain.entity.ExecutionRecord;
import com.galoong.aiprompttracker.domain.repository.ExecutionRepository;
import com.galoong.aiprompttracker.tracking.context.ExecutionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JPA implementation of ExecutionStore that persists to database.
 *
 * <p>Used when JDBC persistence mode is enabled.
 */
@Slf4j
@RequiredArgsConstructor
public class JpaExecutionStore implements ExecutionStore {

    private final ExecutionRepository executionRepository;

    @Override
    public void save(ExecutionContext context) {
        try {
            ExecutionRecord record = convertToEntity(context);
            executionRepository.save(record);

            log.info("Persisted execution {}: function={}, calls={}, totalCost={}, status={}",
                    context.getExecutionId(),
                    context.getFunctionName(),
                    context.getCallsCount(),
                    context.getTotalCost(),
                    context.getStatus());

        } catch (Exception e) {
            log.error("Failed to persist execution: {}", context.getExecutionId(), e);
        }
    }

    /**
     * Convert ExecutionContext to ExecutionRecord entity
     */
    private ExecutionRecord convertToEntity(ExecutionContext context) {
        return ExecutionRecord.builder()
                .id(context.getExecutionId())
                .functionName(context.getFunctionName())
                .category(context.getCategory())
                .tags(context.getTags())
                .environment(context.getEnvironment())
                .startedAt(context.getStartTime())
                .finishedAt(context.getEndTime())
                .durationMs(context.getDurationMs())
                .status(context.getStatus())
                .errorMessage(context.getErrorMessage())
                .callsCount(context.getCallsCount())
                .totalCost(context.getTotalCost())
                .totalTokens(context.getTotalTokens())
                .build();
    }
}
