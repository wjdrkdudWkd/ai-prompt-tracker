package com.galoong.aiprompttracker.providers.openai;

import com.galoong.aiprompttracker.core.provider.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * OpenAI API Provider 구현체
 */
@Slf4j
@Component
public class OpenAIProvider implements AIProvider {

    private static final String API_BASE_URL = "https://api.openai.com/v1";
    private static final String[] SUPPORTED_MODELS = {
            "gpt-4", "gpt-4-turbo", "gpt-4-turbo-preview",
            "gpt-3.5-turbo", "gpt-3.5-turbo-16k"
    };

    private final WebClient webClient;
    private final OpenAIPricingLoader pricingLoader;

    public OpenAIProvider(
            @Value("${ai.providers.openai.api-key:}") String apiKey,
            OpenAIPricingLoader pricingLoader) {
        this.pricingLoader = pricingLoader;
        this.webClient = WebClient.builder()
                .baseUrl(API_BASE_URL)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public String getProviderName() {
        return "OpenAI";
    }

    @Override
    public String[] getSupportedModels() {
        return SUPPORTED_MODELS;
    }

    @Override
    public AIProviderResponse execute(String modelName, String prompt, Map<String, Object> parameters) {
        LocalDateTime requestTime = LocalDateTime.now();

        try {
            // 요청 바디 생성
            Map<String, Object> requestBody = buildRequestBody(modelName, prompt, parameters);

            // API 호출
            OpenAIResponse response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(OpenAIResponse.class)
                    .block();

            LocalDateTime responseTime = LocalDateTime.now();

            if (response == null) {
                return buildErrorResponse(modelName, prompt, "No response from OpenAI", requestTime);
            }

            // 사용량 메트릭 계산
            UsageMetrics usage = buildUsageMetrics(response, requestTime, responseTime);

            return AIProviderResponse.builder()
                    .providerName(getProviderName())
                    .modelName(modelName)
                    .prompt(prompt)
                    .content(extractContent(response))
                    .usage(usage)
                    .finishReason(extractFinishReason(response))
                    .requestTime(requestTime)
                    .responseTime(responseTime)
                    .success(true)
                    .parameters(parameters)
                    .rawResponse(convertToMap(response))
                    .build();

        } catch (Exception e) {
            log.error("OpenAI API call failed", e);
            return buildErrorResponse(modelName, prompt, e.getMessage(), requestTime);
        }
    }

    @Override
    public AIProviderResponse executeStream(String modelName, String prompt,
                                           Map<String, Object> parameters,
                                           StreamCallback streamCallback) {
        // 스트리밍 구현은 향후 추가
        log.warn("Streaming not yet implemented, falling back to standard execution");
        return execute(modelName, prompt, parameters);
    }

    @Override
    public PricingModel getPricing(String modelName) {
        return pricingLoader.getPricing(modelName);
    }

    @Override
    public UsageMetrics calculateUsage(AIProviderResponse response) {
        if (response.getUsage() != null) {
            return response.getUsage();
        }

        // 기본 계산 로직
        return UsageMetrics.builder()
                .promptTokens(0)
                .completionTokens(0)
                .totalTokens(0)
                .estimatedCost(BigDecimal.ZERO)
                .build();
    }

    @Override
    public boolean isHealthy() {
        try {
            webClient.get()
                    .uri("/models")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return true;
        } catch (Exception e) {
            log.error("OpenAI health check failed", e);
            return false;
        }
    }

    private Map<String, Object> buildRequestBody(String modelName, String prompt,
                                                  Map<String, Object> parameters) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", modelName);

        // 메시지 형식으로 변환
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        body.put("messages", new Object[]{message});

        // 추가 파라미터 병합
        if (parameters != null) {
            parameters.forEach((key, value) -> {
                if (!key.equals("model") && !key.equals("messages")) {
                    body.put(key, value);
                }
            });
        }

        return body;
    }

    private UsageMetrics buildUsageMetrics(OpenAIResponse response,
                                          LocalDateTime requestTime,
                                          LocalDateTime responseTime) {
        OpenAIResponse.Usage usage = response.getUsage();
        if (usage == null) {
            return null;
        }

        // 가격 정보 조회
        PricingModel pricing = getPricing(response.getModel());
        BigDecimal cost = pricing != null
                ? pricing.calculateCost(usage.getPromptTokens(), usage.getCompletionTokens())
                : BigDecimal.ZERO;

        long responseTimeMs = java.time.Duration.between(requestTime, responseTime).toMillis();

        return UsageMetrics.builder()
                .promptTokens(usage.getPromptTokens())
                .completionTokens(usage.getCompletionTokens())
                .totalTokens(usage.getTotalTokens())
                .estimatedCost(cost)
                .responseTimeMs(responseTimeMs)
                .cacheHit(false)
                .build();
    }

    private String extractContent(OpenAIResponse response) {
        if (response.getChoices() != null && !response.getChoices().isEmpty()) {
            OpenAIResponse.Choice choice = response.getChoices().get(0);
            if (choice.getMessage() != null) {
                return choice.getMessage().getContent();
            }
        }
        return "";
    }

    private String extractFinishReason(OpenAIResponse response) {
        if (response.getChoices() != null && !response.getChoices().isEmpty()) {
            return response.getChoices().get(0).getFinishReason();
        }
        return "unknown";
    }

    private Map<String, Object> convertToMap(OpenAIResponse response) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", response.getId());
        map.put("model", response.getModel());
        map.put("created", response.getCreated());
        return map;
    }

    private AIProviderResponse buildErrorResponse(String modelName, String prompt,
                                                  String errorMessage, LocalDateTime requestTime) {
        return AIProviderResponse.builder()
                .providerName(getProviderName())
                .modelName(modelName)
                .prompt(prompt)
                .success(false)
                .errorMessage(errorMessage)
                .requestTime(requestTime)
                .responseTime(LocalDateTime.now())
                .build();
    }
}
