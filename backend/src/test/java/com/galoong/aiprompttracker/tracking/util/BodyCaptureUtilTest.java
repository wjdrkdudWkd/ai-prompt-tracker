package com.galoong.aiprompttracker.tracking.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BodyCaptureUtil
 */
class BodyCaptureUtilTest {

    @Test
    void testTruncateString_WithinLimit() {
        String content = "Hello, World!";
        String result = BodyCaptureUtil.truncateString(content, 100, " ...");

        assertEquals(content, result);
    }

    @Test
    void testTruncateString_ExceedsLimit() {
        String content = "This is a very long string that exceeds the limit";
        String result = BodyCaptureUtil.truncateString(content, 20, " ...");

        assertTrue(result.length() <= 24); // 20 + " ..." length
        assertTrue(result.endsWith(" ..."));
        assertFalse(result.contains("limit"));
    }

    @Test
    void testTruncateString_ExactBoundary() {
        String content = "Exact20Characters123";
        String result = BodyCaptureUtil.truncateString(content, 20, " ...");

        assertEquals(content, result);
    }

    @Test
    void testTruncateString_UTF8Handling() {
        // Japanese characters (multi-byte UTF-8)
        String content = "こんにちは世界！これは日本語のテストです。";
        String result = BodyCaptureUtil.truncateString(content, 30, " ...");

        assertNotNull(result);
        assertTrue(result.endsWith(" ..."));
        // Verify no broken UTF-8 characters
        assertFalse(result.contains("�"));
    }

    @Test
    void testTruncateString_NullInput() {
        String result = BodyCaptureUtil.truncateString(null, 100, " ...");
        assertNull(result);
    }

    @Test
    void testTruncateString_EmptyInput() {
        String result = BodyCaptureUtil.truncateString("", 100, " ...");
        assertEquals("", result);
    }

    @Test
    void testTruncateString_NullSuffix() {
        String content = "This is a very long string that exceeds the limit";
        String result = BodyCaptureUtil.truncateString(content, 20, null);

        assertTrue(result.length() <= 20);
        assertFalse(result.endsWith(" ..."));
    }

    @Test
    void testCaptureString_SameAsTruncate() {
        String content = "Test content that is quite long and needs truncation";
        String result = BodyCaptureUtil.captureString(content, 25, " ...");

        assertTrue(result.length() <= 29); // 25 + " ..."
        assertTrue(result.endsWith(" ..."));
    }

    @Test
    void testTruncateString_ByteCount() {
        // Verify it's byte-based, not character-based
        String content = "123456789012345678901234567890"; // 30 chars
        String result = BodyCaptureUtil.truncateString(content, 20, "");

        // Should truncate at 20 bytes
        byte[] bytes = result.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(bytes.length <= 20);
    }
}
