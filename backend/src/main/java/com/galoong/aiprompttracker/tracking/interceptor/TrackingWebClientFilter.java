package com.galoong.aiprompttracker.tracking.interceptor;

import com.galoong.aiprompttracker.tracking.classifier.ModelExtractor;
import com.galoong.aiprompttracker.tracking.classifier.ProviderClassifier;
import com.galoong.aiprompttracker.tracking.collector.CallCollector;
import com.galoong.aiprompttracker.tracking.collector.CallRecordInput;
import com.galoong.aiprompttracker.tracking.context.TrackingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * WebClient filter that automatically tracks AI API calls.
 * Only activates when inside an @AIPrompt execution context.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrackingWebClientFilter implements ExchangeFilterFunction {

    private final ProviderClassifier providerClassifier;
    private final ModelExtractor modelExtractor;
    private final CallCollector callCollector;
    private final UsageMetricsParser usageMetricsParser;

    @Value("${ai-prompts.tracking.store-raw-data:false}")
    private boolean storeRawData;

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        // Only track if we're inside an Execution
        if (!TrackingContext.isTracking()) {
            return next.exchange(request);
        }

        String host = request.url().getHost();
        String path = request.url().getPath();
        String provider = providerClassifier.classifyProvider(host, path);

        // If not a known AI provider, skip tracking
        if ("Unknown".equals(provider)) {
            return next.exchange(request);
        }

        log.debug("Tracking AI call: provider={}, host={}, path={}", provider, host, path);

        long startTime = System.currentTimeMillis();

        // Extract request body (for model detection and raw storage)
        return extractRequestBody(request)
                .flatMap(requestBody -> {
                    String model = modelExtractor.extractModel(requestBody, provider);

                    // Try to extract model from path if not in body
                    if ("unknown".equals(model)) {
                        String pathModel = modelExtractor.extractModelFromPath(path, provider);
                        if (pathModel != null) {
                            model = pathModel;
                        }
                    }

                    final String finalModel = model;
                    final String finalRequestBody = requestBody;

                    return next.exchange(request)
                            .flatMap(response -> handleSuccess(
                                    response, provider, finalModel, finalRequestBody, startTime))
                            .onErrorResume(error -> handleError(
                                    error, provider, finalModel, finalRequestBody, startTime));
                });
    }

    private Mono<String> extractRequestBody(ClientRequest request) {
        // For simple cases, try to read the body
        // Note: This is a simplified version. In production, you might need
        // to use BodyInserters.fromPublisher with buffering
        return Mono.just(""); // TODO: Implement proper body extraction if needed
    }

    private Mono<ClientResponse> handleSuccess(
            ClientResponse response,
            String provider,
            String model,
            String requestBody,
            long startTime) {

        long latency = System.currentTimeMillis() - startTime;

        // Read response body
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .flatMap(responseBody -> {
                    // Parse usage metrics
                    UsageMetricsParser.ParsedUsageMetrics metrics =
                            usageMetricsParser.parse(responseBody, provider);

                    // Record the call
                    CallRecordInput input = CallRecordInput.builder()
                            .provider(provider)
                            .model(model)
                            .promptTokens(metrics.getInputTokens())
                            .completionTokens(metrics.getOutputTokens())
                            .totalTokens(metrics.getTotalTokens())
                            .cost(null) // TODO: Calculate cost based on pricing
                            .latencyMs(latency)
                            .status("success")
                            .requestPreview(storeRawData ? truncate(requestBody, 1000) : null)
                            .responsePreview(storeRawData ? truncate(responseBody, 1000) : null)
                            .rawJson(storeRawData ? responseBody : null)
                            .build();

                    callCollector.recordCall(input);

                    // Return response with body
                    return Mono.just(ClientResponse.create(response.statusCode())
                            .headers(headers -> headers.addAll(response.headers().asHttpHeaders()))
                            .body(responseBody)
                            .build());
                });
    }

    private Mono<ClientResponse> handleError(
            Throwable error,
            String provider,
            String model,
            String requestBody,
            long startTime) {

        long latency = System.currentTimeMillis() - startTime;

        // Record the failed call
        CallRecordInput input = CallRecordInput.builder()
                .provider(provider)
                .model(model)
                .latencyMs(latency)
                .status("error")
                .errorType(error.getClass().getSimpleName())
                .errorMessage(error.getMessage())
                .requestPreview(storeRawData ? truncate(requestBody, 1000) : null)
                .build();

        callCollector.recordCall(input);

        return Mono.error(error);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "... (truncated)";
    }
}
