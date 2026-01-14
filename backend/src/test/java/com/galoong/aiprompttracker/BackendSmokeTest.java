package com.galoong.aiprompttracker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for backend demo application.
 *
 * Verifies that the tracker-starter is properly integrated:
 * - UI is served at /aiprompt-tracker
 * - API is accessible at /aiprompt-tracker/api/**
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BackendSmokeTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void uiEndpointShouldReturnHtml() {
        // Given: The application is running with tracker-starter

        // When: Requesting the UI endpoint
        ResponseEntity<String> response = restTemplate.getForEntity("/aiprompt-tracker/", String.class);

        // Then: Should return 200 OK with HTML content
        // Note: Accepts BOTH React dashboard AND vanilla fallback (dashboard-mvp.html)
        // This ensures zero-config principle - starter works without frontend build
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
            .isNotNull()
            .containsIgnoringCase("<!DOCTYPE html>");

        // Log which UI is being served for diagnostic purposes
        String body = response.getBody();
        if (body != null) {
            if (body.contains("__next") || body.contains("_next")) {
                System.out.println("✅ Serving React dashboard (embedded build)");
            } else {
                System.out.println("✅ Serving vanilla fallback (dashboard-mvp.html)");
            }
        }
    }

    @Test
    void dashboardApiShouldReturnJson() {
        // Given: The application is running with tracker-starter and persistence enabled

        // When: Requesting the dashboard summary API
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/aiprompt-tracker/api/dashboard/summary",
            String.class
        );

        // Then: Should return 200 OK with JSON content
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
            .isNotNull()
            .startsWith("{")
            .contains("totalExecutions");
    }

    @Test
    void demoTriggerEndpointShouldWork() {
        // Given: The demo controller is available

        // When: Requesting the demo trigger endpoint
        ResponseEntity<String> response = restTemplate.getForEntity("/demo/trigger", String.class);

        // Then: Should return 200 OK with success response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
            .isNotNull()
            .contains("success");
    }

    /**
     * CRITICAL TEST: Verify static resources are served correctly (not intercepted by controller)
     *
     * <p>This test addresses the white-screen bug where _next/static/*.js requests
     * were returning HTML (from UiRedirectController) instead of JavaScript.
     *
     * <p>The fix: ApiAutoConfiguration.addResourceHandlers() explicitly registers
     * resource handlers for /aiprompt-tracker/_next/** and *.js, which are checked
     * BEFORE the controller's /** mapping.
     */
    @Test
    void staticJavaScriptFilesShouldReturnJavaScriptNotHtml() {
        // Given: The React dashboard is embedded in the JAR (or may not be - zero-config)

        // When: Requesting a JS file directly (simulating browser loading _next/static/*.js)
        // Note: We can't test exact _next/static paths since they have hashes, so we test
        // the pattern that should work for ANY .js file under /aiprompt-tracker/
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/aiprompt-tracker/test.js",  // Pattern test - any .js should be served by resource handler
            String.class
        );

        // Then: Should either return 404 (file doesn't exist) OR return JavaScript (NOT HTML)
        // The critical assertion: MUST NOT return HTML with __next_error__
        if (response.getStatusCode() == HttpStatus.OK) {
            // If file exists, must be JavaScript
            assertThat(response.getBody())
                .describedAs("JS file should return JavaScript, not HTML")
                .isNotNull()
                .doesNotStartWith("<!DOCTYPE html>")
                .doesNotStartWith("<html")
                .doesNotContain("__next_error__");

            // Verify content-type is NOT text/html
            String contentType = response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
            assertThat(contentType)
                .describedAs("JS file content-type should be application/javascript or similar, not text/html")
                .isNotNull()
                .doesNotContain("text/html");
        } else {
            // 404 is acceptable (file doesn't exist) - the important thing is it's NOT returning HTML
            assertThat(response.getStatusCode()).isIn(HttpStatus.NOT_FOUND);
            System.out.println("✅ Static .js file correctly returns 404 (not intercepted by controller)");
        }
    }

    /**
     * CRITICAL TEST: Verify _next/** paths are handled by resource handlers
     *
     * <p>This is the specific path pattern that was causing the white-screen bug.
     * _next/static/*.js MUST be served by resource handlers, not the controller.
     */
    @Test
    void nextStaticPathsShouldNotReturnHtml() {
        // Given: The React dashboard may or may not be embedded (zero-config)

        // When: Requesting a path under _next/ (simulating browser loading Next.js chunks)
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/aiprompt-tracker/_next/static/test.js",
            String.class
        );

        // Then: Should either 404 (file doesn't exist) OR return JavaScript (NOT HTML)
        if (response.getStatusCode() == HttpStatus.OK) {
            assertThat(response.getBody())
                .describedAs("_next/*.js should return JavaScript, not HTML from controller")
                .isNotNull()
                .doesNotStartWith("<!DOCTYPE html>")
                .doesNotStartWith("<html")
                .doesNotContain("__next_error__");

            String contentType = response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
            assertThat(contentType)
                .describedAs("_next/*.js content-type must not be text/html")
                .isNotNull()
                .doesNotContain("text/html");
        } else {
            assertThat(response.getStatusCode()).isIn(HttpStatus.NOT_FOUND);
            System.out.println("✅ _next/static/*.js correctly returns 404 (not intercepted by controller)");
        }
    }

    /**
     * Verify that SPA routes (without extensions) still return HTML
     *
     * <p>This ensures the controller still handles SPA routing correctly
     * for paths like /dashboard/, /functions/, etc.
     */
    @Test
    void spaRoutesShouldStillReturnHtml() {
        // Given: The dashboard UI is available

        // When: Requesting an SPA route (no file extension)
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/aiprompt-tracker/dashboard/",
            String.class
        );

        // Then: Should return HTML (either React or fallback)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
            .isNotNull()
            .containsIgnoringCase("<!DOCTYPE html>");

        System.out.println("✅ SPA routes correctly return HTML");
    }
}
