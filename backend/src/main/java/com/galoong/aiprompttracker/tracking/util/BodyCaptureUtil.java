package com.galoong.aiprompttracker.tracking.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Utility class for capturing and truncating HTTP request/response bodies.
 * Enforces memory limits to prevent OOM issues.
 *
 * SAFETY: Uses DataBufferUtils for proper buffer lifecycle management.
 */
@Slf4j
public class BodyCaptureUtil {

    /**
     * Capture up to maxBytes from a DataBuffer flux with SAFE buffer handling.
     *
     * This implementation:
     * 1. Uses DataBufferUtils.join() to safely aggregate buffers
     * 2. Enforces a hard cap (maxInMemoryBytes) to prevent OOM
     * 3. Properly releases all buffers
     * 4. Returns BOTH captured preview AND full body for reconstruction
     *
     * @param dataBuffers Original data buffer flux
     * @param maxBytes Maximum bytes to capture for preview
     * @param maxInMemoryBytes Hard cap - if exceeded, returns empty
     * @param truncationSuffix Suffix to append when truncated
     * @return CapturedBody containing preview, full body bytes, and truncation flag
     */
    public static Mono<CapturedBody> captureDataBuffers(
            Flux<DataBuffer> dataBuffers,
            int maxBytes,
            int maxInMemoryBytes,
            String truncationSuffix) {

        // Use DataBufferUtils.join() to safely aggregate the full body
        return DataBufferUtils.join(dataBuffers, maxInMemoryBytes)
                .flatMap(fullBuffer -> {
                    try {
                        int totalBytes = fullBuffer.readableByteCount();

                        // Check if we hit the hard cap
                        if (totalBytes >= maxInMemoryBytes) {
                            log.warn("Response body exceeded maxInMemoryBytes ({}). Skipping capture.", maxInMemoryBytes);
                            DataBufferUtils.release(fullBuffer);
                            return Mono.just(new CapturedBody(
                                    "[Response too large to capture]",
                                    new byte[0],
                                    true
                            ));
                        }

                        // Read full body into byte array
                        byte[] fullBodyBytes = new byte[totalBytes];
                        fullBuffer.read(fullBodyBytes);

                        // Create preview (truncated to maxBytes)
                        boolean wasTruncated = totalBytes > maxBytes;
                        int previewLength = Math.min(totalBytes, maxBytes);

                        String preview = new String(fullBodyBytes, 0, previewLength, StandardCharsets.UTF_8);

                        // Ensure UTF-8 safety at truncation boundary
                        if (wasTruncated) {
                            preview = ensureValidUtf8(preview);
                            if (truncationSuffix != null) {
                                preview += truncationSuffix;
                            }
                        }

                        return Mono.just(new CapturedBody(preview, fullBodyBytes, wasTruncated));

                    } catch (Exception e) {
                        log.error("Error capturing data buffers: {}", e.getMessage(), e);
                        return Mono.just(new CapturedBody("[Capture error]", new byte[0], false));
                    } finally {
                        // CRITICAL: Release the buffer
                        DataBufferUtils.release(fullBuffer);
                    }
                })
                .onErrorResume(ex -> {
                    log.error("Failed to join data buffers: {}", ex.getMessage(), ex);
                    return Mono.just(new CapturedBody("[Capture failed]", new byte[0], false));
                });
    }

    /**
     * Truncate a string to maxBytes (UTF-8 byte count).
     * Ensures we don't split multi-byte UTF-8 characters.
     *
     * @param content Original content
     * @param maxBytes Maximum bytes
     * @param truncationSuffix Suffix to append
     * @return Truncated string
     */
    public static String truncateString(String content, int maxBytes, String truncationSuffix) {
        if (content == null) {
            return null;
        }

        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= maxBytes) {
            return content;
        }

        // Truncate at byte boundary
        String truncated = new String(bytes, 0, maxBytes, StandardCharsets.UTF_8);

        // Ensure valid UTF-8 at end
        truncated = ensureValidUtf8(truncated);

        return truncated + (truncationSuffix != null ? truncationSuffix : "");
    }

    /**
     * Ensure string doesn't end with broken UTF-8 character
     */
    private static String ensureValidUtf8(String str) {
        if (str.isEmpty()) {
            return str;
        }

        // Check if last character is a broken surrogate or replacement character
        char lastChar = str.charAt(str.length() - 1);
        if (Character.isHighSurrogate(lastChar) || lastChar == '\uFFFD') {
            return str.substring(0, str.length() - 1);
        }

        return str;
    }

    /**
     * Result of body capture operation
     */
    public static class CapturedBody {
        private final String preview;
        private final byte[] fullBodyBytes;
        private final boolean wasTruncated;

        public CapturedBody(String preview, byte[] fullBodyBytes, boolean wasTruncated) {
            this.preview = preview;
            this.fullBodyBytes = fullBodyBytes;
            this.wasTruncated = wasTruncated;
        }

        public String getPreview() {
            return preview;
        }

        public byte[] getFullBodyBytes() {
            return fullBodyBytes;
        }

        public boolean wasTruncated() {
            return wasTruncated;
        }

        /**
         * Create a new Flux from the captured full body bytes
         */
        public Flux<DataBuffer> toFlux() {
            if (fullBodyBytes.length == 0) {
                return Flux.empty();
            }

            DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
            DataBuffer buffer = factory.wrap(fullBodyBytes);
            return Flux.just(buffer);
        }
    }

    /**
     * Simple capture of string body with byte limit
     */
    public static String captureString(String body, int maxBytes, String truncationSuffix) {
        return truncateString(body, maxBytes, truncationSuffix);
    }
}
