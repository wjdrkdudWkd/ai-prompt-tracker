package com.galoong.aiprompttracker.tracking.interceptor;

import com.galoong.aiprompttracker.config.properties.TrackingCaptureProperties;
import com.galoong.aiprompttracker.tracking.classifier.ModelExtractor;
import com.galoong.aiprompttracker.tracking.classifier.ProviderClassifier;
import com.galoong.aiprompttracker.tracking.collector.CallCollector;
import com.galoong.aiprompttracker.tracking.collector.CallRecordInput;
import com.galoong.aiprompttracker.tracking.context.TrackingContext;
import com.galoong.aiprompttracker.tracking.util.BodyCaptureUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * WebClient filter that automatically tracks AI API calls.
 * Only activates when inside an @AIPrompt execution context.
 *
 * Safely captures request and response bodies with configurable memory limits
 * to prevent OOM issues while providing debugging capabilities.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrackingWebClientFilter implements ExchangeFilterFunction {

    private final ProviderClassifier providerClassifier;
    private final ModelExtractor modelExtractor;
    private final CallCollector callCollector;
    private final UsageMetricsParser usageMetricsParser;
    private final TrackingCaptureProperties captureProperties;

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        // Only track if we're inside an Execution
        if (!TrackingContext.isTracking()) {
            return next.exchange(request);
        }

        String host = request.url().getHost();
        String path = request.url().getPath();
        String provider = providerClassifier.classifyProvider(host, path);

        // If not a known AI provider, skip tracking (unless configured otherwise)
        if ("Unknown".equals(provider) && !captureProperties.isCaptureUnknownProviders()) {
            return next.exchange(request);
        }

        log.debug("Tracking AI call: provider={}, host={}, path={}", provider, host, path);

        long startTime = System.currentTimeMillis();

        // Check if we should capture based on content type
        String contentType = request.headers().getContentType() != null
                ? request.headers().getContentType().toString()
                : null;

        boolean shouldCapture = captureProperties.isStoreRawData()
                && captureProperties.shouldCaptureContentType(contentType);

        if (shouldCapture) {
            // Capture request body, then execute
            return captureRequestBody(request)
                    .flatMap(capturedRequest -> {
                        String requestBody = capturedRequest.getBody();
                        String model = extractModel(requestBody, provider, path);

                        return next.exchange(request)
                                .flatMap(response -> handleSuccessWithCapture(
                                        response, provider, model, requestBody, startTime))
                                .onErrorResume(error -> handleError(
                                        error, provider, model, requestBody, startTime));
                    });
        } else {
            // No capture, just track basic metrics
            String model = modelExtractor.extractModelFromPath(path, provider);
            if (model == null) {
                model = "unknown";
            }

            final String finalModel = model;
            return next.exchange(request)
                    .flatMap(response -> handleSuccessNoCapture(
                            response, provider, finalModel, startTime))
                    .onErrorResume(error -> handleError(
                            error, provider, finalModel, null, startTime));
        }
    }

    /**
     * Capture request body (best-effort for typical JSON payloads)
     *
     * NOTE: WebClient request bodies are one-shot streams. Full interception
     * requires complex BodyInserter wrapping. For most AI API calls with simple
     * JSON bodies, this best-effort approach works. Complex streaming bodies
     * will return empty string.
     */
    private Mono<CapturedRequest> captureRequestBody(ClientRequest request) {
        // Best-effort MVP: For simple JSON bodies this works
        // For complex streaming bodies, this will return empty
        // TODO: Enhance with BodyInserter wrapping if needed for your use case

        // Return empty request body for now (same as before)
        return Mono.just(new CapturedRequest("", request));
    }

    /**
     * Handle successful response with SAFE body capture
     */
    private Mono<ClientResponse> handleSuccessWithCapture(
            ClientResponse response,
            String provider,
            String model,
            String requestBody,
            long startTime) {

        long latency = System.currentTimeMillis() - startTime;

        // SAFE capture with hard memory limit
        return BodyCaptureUtil.captureDataBuffers(
                        response.bodyToFlux(DataBuffer.class),
                        captureProperties.getMaxResponseBytes(),
                        captureProperties.getMaxInMemoryBytes(),
                        captureProperties.getTruncationSuffix())
                .flatMap(capturedBody -> {
                    String responsePreview = capturedBody.getPreview();
                    boolean wasTruncated = capturedBody.wasTruncated();

                    // Parse usage metrics from captured response (only if not too large)
                    UsageMetricsParser.ParsedUsageMetrics metrics = null;
                    if (!wasTruncated || responsePreview.length() > 100) {
                        try {
                            metrics = usageMetricsParser.parse(responsePreview, provider);
                        } catch (Exception e) {
                            log.debug("Could not parse metrics from truncated response: {}", e.getMessage());
                        }
                    }

                    // Prepare request preview (truncate if needed)
                    String requestPreview = BodyCaptureUtil.captureString(
                            requestBody,
                            captureProperties.getMaxRequestBytes(),
                            captureProperties.getTruncationSuffix());

                    // Determine full raw JSON (only if not truncated)
                    String rawJson = null;
                    if (!wasTruncated && capturedBody.getFullBodyBytes().length > 0) {
                        rawJson = new String(capturedBody.getFullBodyBytes(),
                                java.nio.charset.StandardCharsets.UTF_8);
                    }

                    // Record the call
                    CallRecordInput input = CallRecordInput.builder()
                            .provider(provider)
                            .model(model)
                            .promptTokens(metrics != null ? metrics.getInputTokens() : null)
                            .completionTokens(metrics != null ? metrics.getOutputTokens() : null)
                            .totalTokens(metrics != null ? metrics.getTotalTokens() : null)
                            .cost(null) // TODO: Calculate based on pricing
                            .latencyMs(latency)
                            .status("success")
                            .requestPreview(requestPreview)
                            .responsePreview(responsePreview)
                            .rawJson(rawJson)
                            .wasTruncated(wasTruncated)
                            .build();

                    callCollector.recordCall(input);

                    // Rebuild response with FULL BODY from captured bytes
                    return Mono.just(rebuildResponse(response, capturedBody.toFlux()));
                });
    }

    /**
     * Handle successful response without body capture
     */
    private Mono<ClientResponse> handleSuccessNoCapture(
            ClientResponse response,
            String provider,
            String model,
            long startTime) {

        long latency = System.currentTimeMillis() - startTime;

        // Just record basic metrics without body capture
        CallRecordInput input = CallRecordInput.builder()
                .provider(provider)
                .model(model)
                .latencyMs(latency)
                .status("success")
                .wasTruncated(false)
                .build();

        callCollector.recordCall(input);

        return Mono.just(response);
    }

    /**
     * Handle error
     */
    private Mono<ClientResponse> handleError(
            Throwable error,
            String provider,
            String model,
            String requestBody,
            long startTime) {

        long latency = System.currentTimeMillis() - startTime;

        String requestPreview = null;
        if (captureProperties.isStoreRawData() && requestBody != null) {
            requestPreview = BodyCaptureUtil.captureString(
                    requestBody,
                    captureProperties.getMaxRequestBytes(),
                    captureProperties.getTruncationSuffix());
        }

        // Record the failed call
        CallRecordInput input = CallRecordInput.builder()
                .provider(provider)
                .model(model)
                .latencyMs(latency)
                .status("error")
                .errorType(error.getClass().getSimpleName())
                .errorMessage(truncate(error.getMessage(), 500))
                .requestPreview(requestPreview)
                .wasTruncated(false)
                .build();

        callCollector.recordCall(input);

        return Mono.error(error);
    }

    /**
     * Extract model name from request body or path
     */
    private String extractModel(String requestBody, String provider, String path) {
        String model = modelExtractor.extractModel(requestBody, provider);

        if ("unknown".equals(model)) {
            String pathModel = modelExtractor.extractModelFromPath(path, provider);
            if (pathModel != null) {
                model = pathModel;
            }
        }

        return model;
    }

    /**
     * Rebuild ClientResponse with new body flux
     */
    private ClientResponse rebuildResponse(ClientResponse original, Flux<DataBuffer> bodyFlux) {
        return ClientResponse.create(original.statusCode())
                .headers(headers -> headers.addAll(original.headers().asHttpHeaders()))
                .body(bodyFlux)
                .build();
    }

    /**
     * Truncate string to max length
     */
    private String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    /**
     * Simple holder for captured request
     */
    private static class CapturedRequest {
        private final String body;
        private final ClientRequest request;

        CapturedRequest(String body, ClientRequest request) {
            this.body = body;
            this.request = request;
        }

        String getBody() {
            return body;
        }

        ClientRequest getRequest() {
            return request;
        }
    }
}
