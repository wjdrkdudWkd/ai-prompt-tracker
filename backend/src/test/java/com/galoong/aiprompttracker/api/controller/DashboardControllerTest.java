package com.galoong.aiprompttracker.api.controller;

import com.galoong.aiprompttracker.api.dto.DashboardSummaryResponse;
import com.galoong.aiprompttracker.api.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for DashboardController
 */
@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @Test
    void testGetSummary_withoutFilters() throws Exception {
        // Given
        DashboardSummaryResponse mockResponse = DashboardSummaryResponse.builder()
                .totalCost(125.50)
                .totalExecutions(100L)
                .totalCalls(350L)
                .avgLatencyMs(245.5)
                .avgCallsPerExecution(3.5)
                .executionErrorRate(0.05)
                .callErrorRate(0.02)
                .build();

        when(dashboardService.getSummary(any(), any(), any())).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCost").value(125.50))
                .andExpect(jsonPath("$.totalExecutions").value(100))
                .andExpect(jsonPath("$.totalCalls").value(350))
                .andExpect(jsonPath("$.avgLatencyMs").value(245.5))
                .andExpect(jsonPath("$.avgCallsPerExecution").value(3.5))
                .andExpect(jsonPath("$.executionErrorRate").value(0.05))
                .andExpect(jsonPath("$.callErrorRate").value(0.02));
    }

    @Test
    void testGetSummary_withTimeRangeFilter() throws Exception {
        // Given
        DashboardSummaryResponse mockResponse = DashboardSummaryResponse.builder()
                .totalCost(50.00)
                .totalExecutions(40L)
                .totalCalls(150L)
                .avgLatencyMs(200.0)
                .avgCallsPerExecution(3.75)
                .executionErrorRate(0.0)
                .callErrorRate(0.0)
                .build();

        when(dashboardService.getSummary(any(Instant.class), any(Instant.class), any()))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/api/dashboard/summary")
                        .param("from", "2024-01-01T00:00:00Z")
                        .param("to", "2024-01-31T23:59:59Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCost").value(50.00))
                .andExpect(jsonPath("$.totalExecutions").value(40));
    }

    @Test
    void testGetSummary_withEnvironmentFilter() throws Exception {
        // Given
        DashboardSummaryResponse mockResponse = DashboardSummaryResponse.builder()
                .totalCost(75.25)
                .totalExecutions(60L)
                .totalCalls(200L)
                .avgLatencyMs(220.0)
                .avgCallsPerExecution(3.33)
                .executionErrorRate(0.03)
                .callErrorRate(0.01)
                .build();

        when(dashboardService.getSummary(any(), any(), any())).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/api/dashboard/summary")
                        .param("env", "production"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCost").value(75.25))
                .andExpect(jsonPath("$.totalExecutions").value(60));
    }
}
