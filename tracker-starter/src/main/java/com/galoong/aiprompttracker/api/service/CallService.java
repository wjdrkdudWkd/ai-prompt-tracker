package com.galoong.aiprompttracker.api.service;

import com.galoong.aiprompttracker.api.config.ApiAutoConfiguration;
import com.galoong.aiprompttracker.api.dto.CallResponse;
import com.galoong.aiprompttracker.domain.entity.CallRecord;
import com.galoong.aiprompttracker.domain.entity.ExecutionRecord;
import com.galoong.aiprompttracker.domain.repository.CallRepositoryExtended;
import com.galoong.aiprompttracker.domain.repository.ExecutionRepositoryExtended;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for call-level queries
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CallService {

    private final CallRepositoryExtended callRepository;
    private final ExecutionRepositoryExtended executionRepository;
    private final ApiAutoConfiguration.PersistenceGuard persistenceGuard;

    /**
     * Search calls with filters
     */
    public Page<CallResponse> searchCalls(
            String provider,
            String model,
            String status,
            Instant from,
            Instant to,
            Pageable pageable) {

        persistenceGuard.requirePersistence();

        Page<CallRecord> calls;

        if (provider != null && from != null && to != null) {
            calls = callRepository.findByProviderAndCreatedAtBetween(provider, from, to, pageable);
        } else if (model != null && from != null && to != null) {
            calls = callRepository.findByModelAndCreatedAtBetween(model, from, to, pageable);
        } else if (status != null && from != null && to != null) {
            calls = callRepository.findByStatusAndCreatedAtBetween(status, from, to, pageable);
        } else {
            calls = callRepository.findAll(pageable);
        }

        // Build execution ID to function name map to avoid N+1
        Map<String, String> executionIdToFunctionName = new HashMap<>();

        return calls.map(c -> {
            // Get function name from execution (cached to avoid N+1)
            String functionName = executionIdToFunctionName.get(c.getExecutionId());
            if (functionName == null) {
                ExecutionRecord execution = executionRepository.findById(c.getExecutionId()).orElse(null);
                functionName = execution != null ? execution.getFunctionName() : "unknown";
                executionIdToFunctionName.put(c.getExecutionId(), functionName);
            }

            return CallResponse.builder()
                    .callId(c.getId())
                    .executionId(c.getExecutionId())
                    .functionName(functionName)
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
                    .build();
        });
    }
}
