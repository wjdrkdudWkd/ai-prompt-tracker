package com.galoong.aiprompttracker.tracking.interceptor;

import com.galoong.aiprompttracker.config.properties.TrackingCaptureProperties;
import com.galoong.aiprompttracker.tracking.classifier.ModelExtractor;
import com.galoong.aiprompttracker.tracking.classifier.ProviderClassifier;
import com.galoong.aiprompttracker.tracking.collector.CallCollector;
import com.galoong.aiprompttracker.tracking.collector.CallRecordInput;
import com.galoong.aiprompttracker.tracking.context.TrackingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.Buffer;
import okio.BufferedSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * OkHttp interceptor for tracking AI API calls.
 *
 * <p>Only activates when inside an @AIPrompt execution context.
 * Safely captures request/response bodies with memory limits.
 *
 * <p>Usage:
 * <pre>
 * OkHttpClient client = new OkHttpClient.Builder()
 *     .addInterceptor(trackingOkHttpInterceptor)
 *     .build();
 * </pre>
 */
@Slf4j
@RequiredArgsConstructor
public class TrackingOkHttpInterceptor implements Interceptor {

    private final ProviderClassifier providerClassifier;
    private final ModelExtractor modelExtractor;
    private final CallCollector callCollector;
    private final UsageMetricsParser usageMetricsParser;
    private final TrackingCaptureProperties captureProperties;

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        // Only track if inside @AIPrompt execution
        if (!TrackingContext.isTracking()) {
            return chain.proceed(request);
        }

        HttpUrl url = request.url();
        String host = url.host();
        String path = url.encodedPath();
        String provider = providerClassifier.classifyProvider(host, path);

        // Skip unknown providers unless configured
        if ("Unknown".equals(provider) && !captureProperties.isCaptureUnknownProviders()) {
            return chain.proceed(request);
        }

        log.debug("Tracking OkHttp call: provider={}, host={}, path={}", provider, host, path);

        long startTime = System.currentTimeMillis();

        // Check if we should capture bodies
        String contentType = getContentType(request);
        boolean shouldCapture = captureProperties.isStoreRawData()
                && captureProperties.shouldCaptureContentType(contentType);

        if (shouldCapture) {
            return executeWithCapture(chain, request, provider, path, startTime);
        } else {
            return executeWithoutCapture(chain, request, provider, path, startTime);
        }
    }

    /**
     * Execute with body capture
     */
    private Response executeWithCapture(Chain chain, Request request, String provider,
                                         String path, long startTime) throws IOException {
        // Capture request body (best-effort)
        String requestBody = captureRequestBody(request);
        String model = extractModel(requestBody, provider, path);

        // Execute request
        Response response;
        try {
            response = chain.proceed(request);
        } catch (IOException e) {
            // Record failed call
            long latency = System.currentTimeMillis() - startTime;
            recordError(provider, model, requestBody, latency, e);
            throw e;
        }

        long latency = System.currentTimeMillis() - startTime;

        // Capture response body safely
        CapturedResponse captured = captureResponseBody(response);

        // Parse metrics from captured response
        UsageMetricsParser.ParsedUsageMetrics metrics = null;
        if (!captured.wasTruncated || captured.preview.length() > 100) {
            try {
                metrics = usageMetricsParser.parse(captured.preview, provider);
            } catch (Exception e) {
                log.debug("Could not parse metrics from response: {}", e.getMessage());
            }
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
                .status(response.isSuccessful() ? "success" : "error")
                .requestPreview(truncateString(requestBody, captureProperties.getMaxRequestBytes()))
                .responsePreview(captured.preview)
                .rawJson(captured.rawJson)
                .wasTruncated(captured.wasTruncated)
                .build();

        callCollector.recordCall(input);

        // Return response with original body reconstructed
        return captured.response;
    }

    /**
     * Execute without body capture (metadata only)
     */
    private Response executeWithoutCapture(Chain chain, Request request, String provider,
                                            String path, long startTime) throws IOException {
        String model = modelExtractor.extractModelFromPath(path, provider);
        if (model == null) {
            model = "unknown";
        }

        Response response;
        try {
            response = chain.proceed(request);
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
                .status(response.isSuccessful() ? "success" : "error")
                .wasTruncated(false)
                .build();

        callCollector.recordCall(input);

        return response;
    }

    /**
     * Capture request body (best-effort for JSON)
     */
    private String captureRequestBody(Request request) {
        RequestBody body = request.body();
        if (body == null) {
            return "";
        }

        try {
            // Only capture if content type is allowed
            MediaType mediaType = body.contentType();
            if (mediaType != null && !captureProperties.shouldCaptureContentType(mediaType.toString())) {
                return "";
            }

            // Buffer the request body
            Buffer buffer = new Buffer();
            body.writeTo(buffer);

            // Read up to max bytes
            long contentLength = body.contentLength();
            if (contentLength > captureProperties.getMaxInMemoryBytes()) {
                log.debug("Request body too large ({} bytes), skipping capture", contentLength);
                return "[Request too large to capture]";
            }

            int bytesToRead = (int) Math.min(contentLength != -1 ? contentLength : Integer.MAX_VALUE,
                    captureProperties.getMaxRequestBytes());

            byte[] bytes = new byte[bytesToRead];
            buffer.read(bytes, 0, bytesToRead);

            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.debug("Failed to capture request body: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Capture response body safely with full reconstruction
     */
    private CapturedResponse captureResponseBody(Response response) throws IOException {
        ResponseBody originalBody = response.body();
        if (originalBody == null) {
            return new CapturedResponse(response, "", null, false);
        }

        // Check content type
        MediaType mediaType = originalBody.contentType();
        if (mediaType != null && !captureProperties.shouldCaptureContentType(mediaType.toString())) {
            return new CapturedResponse(response, "", null, false);
        }

        // Check content length against hard cap
        long contentLength = originalBody.contentLength();
        if (contentLength > captureProperties.getMaxInMemoryBytes()) {
            log.warn("Response body too large ({} bytes), skipping capture", contentLength);
            return new CapturedResponse(response, "[Response too large to capture]", null, true);
        }

        // Read full body into byte array
        BufferedSource source = originalBody.source();
        source.request(Long.MAX_VALUE); // Buffer entire body
        Buffer buffer = source.getBuffer();

        byte[] fullBodyBytes = buffer.clone().readByteArray();

        // Create preview (truncated to maxResponseBytes)
        boolean wasTruncated = fullBodyBytes.length > captureProperties.getMaxResponseBytes();
        int previewLength = Math.min(fullBodyBytes.length, captureProperties.getMaxResponseBytes());

        String preview = new String(fullBodyBytes, 0, previewLength, StandardCharsets.UTF_8);
        if (wasTruncated) {
            preview = ensureValidUtf8(preview);
            if (captureProperties.getTruncationSuffix() != null) {
                preview += captureProperties.getTruncationSuffix();
            }
        }

        // Store raw JSON only if not truncated
        String rawJson = null;
        if (!wasTruncated && fullBodyBytes.length > 0) {
            rawJson = new String(fullBodyBytes, StandardCharsets.UTF_8);
        }

        // Rebuild response with new body containing full bytes
        ResponseBody newBody = ResponseBody.create(fullBodyBytes, mediaType);
        Response newResponse = response.newBuilder()
                .body(newBody)
                .build();

        return new CapturedResponse(newResponse, preview, rawJson, wasTruncated);
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
     * Get content type from request
     */
    private String getContentType(Request request) {
        RequestBody body = request.body();
        if (body == null) {
            return null;
        }
        MediaType mediaType = body.contentType();
        return mediaType != null ? mediaType.toString() : null;
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
     * Captured response holder
     */
    private static class CapturedResponse {
        final Response response;
        final String preview;
        final String rawJson;
        final boolean wasTruncated;

        CapturedResponse(Response response, String preview, String rawJson, boolean wasTruncated) {
            this.response = response;
            this.preview = preview;
            this.rawJson = rawJson;
            this.wasTruncated = wasTruncated;
        }
    }
}
