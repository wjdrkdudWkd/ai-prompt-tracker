package com.galoong.aiprompttracker.api.service;

import com.galoong.aiprompttracker.api.config.ApiAutoConfiguration;
import com.galoong.aiprompttracker.api.dto.ExecutionDetailResponse;
import com.galoong.aiprompttracker.domain.entity.CallRecord;
import com.galoong.aiprompttracker.domain.entity.ExecutionRecord;
import com.galoong.aiprompttracker.domain.repository.CallRepositoryExtended;
import com.galoong.aiprompttracker.domain.repository.ExecutionRepositoryExtended;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for execution-level queries
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExecutionService {

    private final ExecutionRepositoryExtended executionRepository;
    private final CallRepositoryExtended callRepository;
    private final ApiAutoConfiguration.PersistenceGuard persistenceGuard;

    /**
     * Get execution detail with call timeline
     */
    public ExecutionDetailResponse getExecutionDetail(String executionId) {
        persistenceGuard.requirePersistence();

        ExecutionRecord execution = executionRepository.findById(executionId)
                .orElse(null);

        if (execution == null) {
            return null;
        }

        // Get calls for this execution
        List<CallRecord> calls = callRepository.findByExecutionIdOrderByCreatedAtAsc(executionId);

        List<ExecutionDetailResponse.CallDetail> callDetails = calls.stream()
                .map(c -> ExecutionDetailResponse.CallDetail.builder()
                        .callId(c.getId())
                        .provider(c.getProvider())
                        .model(c.getModel())
                        .promptTokens(c.getPromptTokens())
                        .completionTokens(c.getCompletionTokens())
                        .totalTokens(c.getTotalTokens())
                        .cost(c.getCost())
                        .latencyMs(c.getLatencyMs())
                        .status(c.getStatus())
                        .errorType(c.getErrorType())
                        .errorMessage(c.getErrorMessage())
                        .wasTruncated(c.getWasTruncated())
                        .requestPreview(c.getRequestPreview())
                        .responsePreview(c.getResponsePreview())
                        .createdAt(c.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return ExecutionDetailResponse.builder()
                .executionId(execution.getId())
                .functionName(execution.getFunctionName())
                .category(execution.getCategory())
                .tags(execution.getTags())
                .environment(execution.getEnvironment())
                .startedAt(execution.getStartedAt())
                .finishedAt(execution.getFinishedAt())
                .durationMs(execution.getDurationMs())
                .status(execution.getStatus())
                .errorMessage(execution.getErrorMessage())
                .callsCount(execution.getCallsCount())
                .totalTokens(execution.getTotalTokens())
                .totalCost(execution.getTotalCost())
                .calls(callDetails)
                .build();
    }
}
