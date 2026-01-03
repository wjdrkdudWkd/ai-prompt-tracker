package com.galoong.aiprompttracker.api.controller;

import com.galoong.aiprompttracker.api.dto.ExecutionDetailResponse;
import com.galoong.aiprompttracker.api.service.ExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for execution-level queries
 */
@Slf4j
@RestController
@RequestMapping("/api/executions")
@RequiredArgsConstructor
public class ExecutionController {

    private final ExecutionService executionService;

    /**
     * GET /api/executions/{executionId}
     *
     * Returns execution detail with call timeline
     */
    @GetMapping("/{executionId}")
    public ResponseEntity<ExecutionDetailResponse> getExecutionDetail(
            @PathVariable String executionId) {

        log.debug("GET /api/executions/{} - executionId={}", executionId, executionId);

        ExecutionDetailResponse detail = executionService.getExecutionDetail(executionId);
        if (detail == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(detail);
    }
}
