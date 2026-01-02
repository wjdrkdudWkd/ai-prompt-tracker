package com.galoong.aiprompttracker.tracking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.galoong.aiprompttracker.core.provider.AIProviderResponse;
import com.galoong.aiprompttracker.core.provider.UsageMetrics;
import com.galoong.aiprompttracker.domain.entity.AICallRecord;
import com.galoong.aiprompttracker.domain.repository.AICallRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * AI 호출 추적 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingService {

    private final AICallRepository callRepository;
    private final ObjectMapper objectMapper;

    /**
     * AI 호출 기록 저장
     */
    @Transactional
    public void recordAICall(AIProviderResponse response, String functionName,
                            String description, String category, String[] tags) {
        try {
            AICallRecord record = buildCallRecord(response, functionName, description, category, tags);
            callRepository.save(record);
            log.debug("Recorded AI call for function: {}", functionName);
        } catch (Exception e) {
            log.error("Failed to record AI call", e);
        }
    }

    private AICallRecord buildCallRecord(AIProviderResponse response, String functionName,
                                        String description, String category, String[] tags) {
        UsageMetrics usage = response.getUsage();

        return AICallRecord.builder()
                .providerName(response.getProviderName())
                .modelName(response.getModelName())
                .functionName(functionName)
                .functionDescription(description)
                .category(category)
                .prompt(response.getPrompt())
                .responseContent(response.getContent())
                .promptTokens(usage != null ? usage.getPromptTokens() : null)
                .completionTokens(usage != null ? usage.getCompletionTokens() : null)
                .totalTokens(usage != null ? usage.calculateTotalTokens() : null)
                .estimatedCost(usage != null ? usage.getEstimatedCost() : null)
                .responseTimeMs(usage != null ? usage.getResponseTimeMs() : null)
                .finishReason(response.getFinishReason())
                .success(response.getSuccess())
                .errorMessage(response.getErrorMessage())
                .cacheHit(usage != null ? usage.getCacheHit() : false)
                .parameters(serializeParameters(response.getParameters()))
                .tags(tags != null ? String.join(",", tags) : null)
                .build();
    }

    private String serializeParameters(Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(parameters);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize parameters", e);
            return null;
        }
    }
}
