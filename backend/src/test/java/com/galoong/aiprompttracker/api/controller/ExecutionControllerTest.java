package com.galoong.aiprompttracker.api.controller;

import com.galoong.aiprompttracker.api.dto.ExecutionDetailResponse;
import com.galoong.aiprompttracker.api.service.ExecutionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for ExecutionController
 */
@WebMvcTest(ExecutionController.class)
class ExecutionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExecutionService executionService;

    @Test
    void testGetExecutionDetail_success() throws Exception {
        // Given
        String executionId = "exec-123";
        Instant now = Instant.now();

        ExecutionDetailResponse.CallDetail callDetail = ExecutionDetailResponse.CallDetail.builder()
                .callId("call-1")
                .provider("OpenAI")
                .model("gpt-4")
                .promptTokens(100)
                .completionTokens(50)
                .totalTokens(150)
                .cost(0.005)
                .latencyMs(250L)
                .status("success")
                .wasTruncated(false)
                .requestPreview("{\"messages\":[...]}")
                .responsePreview("{\"choices\":[...]}")
                .createdAt(now)
                .build();

        ExecutionDetailResponse mockResponse = ExecutionDetailResponse.builder()
                .executionId(executionId)
                .functionName("testFunction")
                .category("default")
                .tags(new String[]{"test"})
                .environment("development")
                .startedAt(now.minusSeconds(10))
                .finishedAt(now)
                .durationMs(10000L)
                .status("success")
                .callsCount(1)
                .totalTokens(150L)
                .totalCost(0.005)
                .calls(List.of(callDetail))
                .build();

        when(executionService.getExecutionDetail(executionId)).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/api/executions/{executionId}", executionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId").value(executionId))
                .andExpect(jsonPath("$.functionName").value("testFunction"))
                .andExpect(jsonPath("$.category").value("default"))
                .andExpect(jsonPath("$.environment").value("development"))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.callsCount").value(1))
                .andExpect(jsonPath("$.totalTokens").value(150))
                .andExpect(jsonPath("$.totalCost").value(0.005))
                .andExpect(jsonPath("$.calls[0].callId").value("call-1"))
                .andExpect(jsonPath("$.calls[0].provider").value("OpenAI"))
                .andExpect(jsonPath("$.calls[0].model").value("gpt-4"))
                .andExpect(jsonPath("$.calls[0].wasTruncated").value(false))
                .andExpect(jsonPath("$.calls[0].totalTokens").value(150))
                .andExpect(jsonPath("$.calls[0].cost").value(0.005));
    }

    @Test
    void testGetExecutionDetail_notFound() throws Exception {
        // Given
        String executionId = "nonexistent";
        when(executionService.getExecutionDetail(executionId)).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/executions/{executionId}", executionId))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetExecutionDetail_withTruncatedCall() throws Exception {
        // Given
        String executionId = "exec-456";
        Instant now = Instant.now();

        ExecutionDetailResponse.CallDetail callDetail = ExecutionDetailResponse.CallDetail.builder()
                .callId("call-2")
                .provider("Anthropic")
                .model("claude-3-opus")
                .promptTokens(1000)
                .completionTokens(500)
                .totalTokens(1500)
                .cost(0.075)
                .latencyMs(1200L)
                .status("success")
                .wasTruncated(true)  // Response was truncated
                .requestPreview("{\"messages\":[...]}")
                .responsePreview("{\"content\":[...] ... (truncated)")
                .createdAt(now)
                .build();

        ExecutionDetailResponse mockResponse = ExecutionDetailResponse.builder()
                .executionId(executionId)
                .functionName("largeResponseFunction")
                .category("default")
                .tags(new String[]{})
                .environment("production")
                .startedAt(now.minusSeconds(5))
                .finishedAt(now)
                .durationMs(5000L)
                .status("success")
                .callsCount(1)
                .totalTokens(1500L)
                .totalCost(0.075)
                .calls(List.of(callDetail))
                .build();

        when(executionService.getExecutionDetail(executionId)).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/api/executions/{executionId}", executionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId").value(executionId))
                .andExpect(jsonPath("$.calls[0].wasTruncated").value(true))
                .andExpect(jsonPath("$.calls[0].responsePreview").value("{\"content\":[...] ... (truncated)"));
    }
}
