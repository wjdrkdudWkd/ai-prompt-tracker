package com.galoong.aiprompttracker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
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
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
            .isNotNull()
            .contains("AI Prompt Tracker")
            .contains("<!DOCTYPE html>");
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
}
