package com.galoong.aiprompttracker.api.controller;

import com.galoong.aiprompttracker.api.dto.ExecutionSummaryResponse;
import com.galoong.aiprompttracker.api.dto.FunctionAggregateResponse;
import com.galoong.aiprompttracker.api.dto.FunctionDetailResponse;
import com.galoong.aiprompttracker.api.service.FunctionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * REST API for function-level statistics
 */
@Slf4j
@RestController
@RequestMapping("/aiprompt-tracker/api/functions")
@RequiredArgsConstructor
public class FunctionController {

    private final FunctionService functionService;

    /**
     * GET /api/functions?from=&to=&env=&category=&status=&q=&page=&size=
     *
     * Returns list of function aggregates
     */
    @GetMapping
    public ResponseEntity<Page<FunctionAggregateResponse>> getFunctions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String env,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("GET /api/functions - from={}, to={}, env={}, category={}, status={}, q={}, page={}, size={}",
                from, to, env, category, status, q, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "totalCost"));
        Page<FunctionAggregateResponse> functions = functionService.getFunctionAggregates(
                from, to, env, category, status, q, pageable);

        return ResponseEntity.ok(functions);
    }

    /**
     * GET /api/functions/{functionName}
     *
     * Returns function detail with provider/model breakdown
     */
    @GetMapping("/{functionName}")
    public ResponseEntity<FunctionDetailResponse> getFunctionDetail(
            @PathVariable String functionName) {

        log.debug("GET /api/functions/{} - functionName={}", functionName, functionName);

        FunctionDetailResponse detail = functionService.getFunctionDetail(functionName);
        if (detail == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(detail);
    }

    /**
     * GET /api/functions/{functionName}/executions?from=&to=&page=&size=
     *
     * Returns execution history for a function
     */
    @GetMapping("/{functionName}/executions")
    public ResponseEntity<Page<ExecutionSummaryResponse>> getFunctionExecutions(
            @PathVariable String functionName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("GET /api/functions/{}/executions - functionName={}, from={}, to={}, page={}, size={}",
                functionName, functionName, from, to, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startedAt"));
        Page<ExecutionSummaryResponse> executions = functionService.getFunctionExecutions(
                functionName, from, to, pageable);

        return ResponseEntity.ok(executions);
    }
}
