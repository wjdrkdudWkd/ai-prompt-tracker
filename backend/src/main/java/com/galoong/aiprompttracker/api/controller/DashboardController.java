package com.galoong.aiprompttracker.api.controller;

import com.galoong.aiprompttracker.api.dto.DashboardSummaryResponse;
import com.galoong.aiprompttracker.api.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * REST API for dashboard statistics
 */
@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * GET /api/dashboard/summary?from=&to=&env=
     *
     * Returns aggregated dashboard statistics
     */
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String env) {

        log.debug("GET /api/dashboard/summary - from={}, to={}, env={}", from, to, env);

        DashboardSummaryResponse summary = dashboardService.getSummary(from, to, env);
        return ResponseEntity.ok(summary);
    }
}
