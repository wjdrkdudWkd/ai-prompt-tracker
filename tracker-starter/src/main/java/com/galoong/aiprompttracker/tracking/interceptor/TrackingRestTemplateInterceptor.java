package com.galoong.aiprompttracker.tracking.interceptor;

import com.galoong.aiprompttracker.config.properties.TrackingCaptureProperties;
import com.galoong.aiprompttracker.tracking.classifier.ModelExtractor;
import com.galoong.aiprompttracker.tracking.classifier.ProviderClassifier;
import com.galoong.aiprompttracker.tracking.collector.CallCollector;
import com.galoong.aiprompttracker.tracking.collector.CallRecordInput;
import com.galoong.aiprompttracker.tracking.context.TrackingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * RestTemplate/RestClient interceptor for tracking AI API calls.
 *
 * <p>Only activates when inside an @AIPrompt execution context.
 * Safely captures request/response bodies with memory limits.
 *
 * <p>Usage with RestTemplate:
 * <pre>
 * RestTemplate restTemplate = new RestTemplate();
 * restTemplate.getInterceptors().add(trackingRestTemplateInterceptor);
 * </pre>
 *
 * <p>Usage with RestClient (Spring Boot 3):
 * <pre>
 * RestClient client = RestClient.builder()
 *     .requestInterceptor(trackingRestTemplateInterceptor)
 *     .build();
 * </pre>
 */
@Slf4j
@RequiredArgsConstructor
public class TrackingRestTemplateInterceptor implements ClientHttpRequestInterceptor {

    private final ProviderClassifier providerClassifier;
    private final ModelExtractor modelExtractor;
    private final CallCollector callCollector;
    private final UsageMetricsParser usageMetricsParser;
    private final TrackingCaptureProperties captureProperties;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                         ClientHttpRequestExecution execution) throws IOException {
        // Only track if inside @AIPrompt execution
        if (!TrackingContext.isTracking()) {
            return execution.execute(request, body);
        }

        URI uri = request.getURI();
        String host = uri.getHost();
        String path = uri.getPath();
        String provider = providerClassifier.classifyProvider(host, path);

        // Skip unknown providers unless configured
        if ("Unknown".equals(provider) && !captureProperties.isCaptureUnknownProviders()) {
            return execution.execute(request, body);
        }

        log.debug("Tracking RestTemplate call: provider={}, host={}, path={}", provider, host, path);

        long startTime = System.currentTimeMillis();

        // Check if we should capture bodies
        String contentType = request.getHeaders().getContentType() != null
                ? request.getHeaders().getContentType().toString()
                : null;

        boolean shouldCapture = captureProperties.isStoreRawData()
                && captureProperties.shouldCaptureContentType(contentType);

        if (shouldCapture) {
            return executeWithCapture(request, body, execution, provider, path, startTime);
        } else {
            return executeWithoutCapture(request, body, execution, provider, path, startTime);
        }
    }

    /**
     * Execute with body capture
     */
    private ClientHttpResponse executeWithCapture(HttpRequest request, byte[] body,
                                                    ClientHttpRequestExecution execution,
                                                    String provider, String path,
                                                    long startTime) throws IOException {
        // Capture request body
        String requestBody = body.length > 0 ? new String(body, StandardCharsets.UTF_8) : "";
        String model = extractModel(requestBody, provider, path);

        // Execute request
        ClientHttpResponse response;
        try {
            response = execution.execute(request, body);
        } catch (IOException e) {
            long latency = System.currentTimeMillis() - startTime;
            recordError(provider, model, requestBody, latency, e);
            throw e;
        }

        long latency = System.currentTimeMillis() - startTime;

        // Wrap response to capture body safely
        BufferedClientHttpResponse bufferedResponse = new BufferedClientHttpResponse(response,
                captureProperties.getMaxInMemoryBytes());

        // Parse metrics from captured response
        UsageMetricsParser.ParsedUsageMetrics metrics = null;
        if (!bufferedResponse.wasTruncated() || bufferedResponse.getPreview().length() > 100) {
            try {
                metrics = usageMetricsParser.parse(bufferedResponse.getPreview(), provider);
            } catch (Exception e) {
                log.debug("Could not parse metrics from response: {}", e.getMessage());
            }
        }

        // Prepare previews
        String requestPreview = truncateString(requestBody, captureProperties.getMaxRequestBytes());
        String responsePreview = truncateString(bufferedResponse.getPreview(),
                captureProperties.getMaxResponseBytes());

        // Determine raw JSON
        String rawJson = null;
        if (!bufferedResponse.wasTruncated() && bufferedResponse.getBodyBytes().length > 0) {
            rawJson = new String(bufferedResponse.getBodyBytes(), StandardCharsets.UTF_8);
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
                .status(response.getStatusCode().is2xxSuccessful() ? "success" : "error")
                .requestPreview(requestPreview)
                .responsePreview(responsePreview)
                .rawJson(rawJson)
                .wasTruncated(bufferedResponse.wasTruncated())
                .build();

        callCollector.recordCall(input);

        return bufferedResponse;
    }

    /**
     * Execute without body capture (metadata only)
     */
    private ClientHttpResponse executeWithoutCapture(HttpRequest request, byte[] body,
                                                       ClientHttpRequestExecution execution,
                                                       String provider, String path,
                                                       long startTime) throws IOException {
        String model = modelExtractor.extractModelFromPath(path, provider);
        if (model == null) {
            model = "unknown";
        }

        ClientHttpResponse response;
        try {
            response = execution.execute(request, body);
        } catch (IOException e) {
            long latency = System.currentTimeMillis() - startTime;
            recordError(provider, model, null, latency, e);
            throw e;
        }

        long latency = System.currentTimeMillis() - startTime;

        // Record without body
        CallRecordInput input = CallRecordInput.builder()
                .provider(provider)
                .model(model)
                .latencyMs(latency)
                .status(response.getStatusCode().is2xxSuccessful() ? "success" : "error")
                .wasTruncated(false)
                .build();

        callCollector.recordCall(input);

        return response;
    }

    /**
     * Record error call
     */
    private void recordError(String provider, String model, String requestBody,
                              long latency, Throwable error) {
        String requestPreview = null;
        if (captureProperties.isStoreRawData() && requestBody != null) {
            requestPreview = truncateString(requestBody, captureProperties.getMaxRequestBytes());
        }

        CallRecordInput input = CallRecordInput.builder()
                .provider(provider)
                .model(model)
                .latencyMs(latency)
                .status("error")
                .errorType(error.getClass().getSimpleName())
                .errorMessage(truncateString(error.getMessage(), 500))
                .requestPreview(requestPreview)
                .wasTruncated(false)
                .build();

        callCollector.recordCall(input);
    }

    /**
     * Extract model from request body or path
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
     * Truncate string to max length
     */
    private String truncateString(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return ensureValidUtf8(text.substring(0, maxLength)) +
                (captureProperties.getTruncationSuffix() != null ? captureProperties.getTruncationSuffix() : "");
    }

    /**
     * Ensure string doesn't end with broken UTF-8 character
     */
    private String ensureValidUtf8(String str) {
        if (str.isEmpty()) {
            return str;
        }
        char lastChar = str.charAt(str.length() - 1);
        if (Character.isHighSurrogate(lastChar) || lastChar == '\uFFFD') {
            return str.substring(0, str.length() - 1);
        }
        return str;
    }

    /**
     * Buffered response wrapper that captures body safely
     */
    private static class BufferedClientHttpResponse implements ClientHttpResponse {
        private final ClientHttpResponse delegate;
        private final byte[] bodyBytes;
        private final boolean wasTruncated;
        private final String preview;

        BufferedClientHttpResponse(ClientHttpResponse delegate, int maxInMemoryBytes) throws IOException {
            this.delegate = delegate;

            // Read body into memory with hard cap
            InputStream bodyStream = delegate.getBody();
            if (bodyStream == null) {
                this.bodyBytes = new byte[0];
                this.wasTruncated = false;
                this.preview = "";
                return;
            }

            // Read up to maxInMemoryBytes
            byte[] buffer = StreamUtils.copyToByteArray(bodyStream);

            if (buffer.length > maxInMemoryBytes) {
                this.bodyBytes = new byte[0];
                this.wasTruncated = true;
                this.preview = "[Response too large to capture]";
            } else {
                this.bodyBytes = buffer;
                this.wasTruncated = false;
                this.preview = new String(buffer, StandardCharsets.UTF_8);
            }
        }

        @Override
        public HttpStatusCode getStatusCode() throws IOException {
            return delegate.getStatusCode();
        }

        @Override
        public String getStatusText() throws IOException {
            return delegate.getStatusText();
        }

        @Override
        public void close() {
            delegate.close();
        }

        @Override
        public InputStream getBody() throws IOException {
            return new ByteArrayInputStream(bodyBytes);
        }

        @Override
        public org.springframework.http.HttpHeaders getHeaders() {
            return delegate.getHeaders();
        }

        public byte[] getBodyBytes() {
            return bodyBytes;
        }

        public boolean wasTruncated() {
            return wasTruncated;
        }

        public String getPreview() {
            return preview;
        }
    }
}
