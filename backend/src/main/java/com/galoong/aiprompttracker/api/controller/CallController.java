package com.galoong.aiprompttracker.api.controller;

import com.galoong.aiprompttracker.api.dto.CallResponse;
import com.galoong.aiprompttracker.api.service.CallService;
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
 * REST API for call-level queries
 */
@Slf4j
@RestController
@RequestMapping("/api/calls")
@RequiredArgsConstructor
public class CallController {

    private final CallService callService;

    /**
     * GET /api/calls?provider=&model=&status=&from=&to=&page=&size=
     *
     * Returns call list with filtering
     */
    @GetMapping
    public ResponseEntity<Page<CallResponse>> searchCalls(
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("GET /api/calls - provider={}, model={}, status={}, from={}, to={}, page={}, size={}",
                provider, model, status, from, to, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CallResponse> calls = callService.searchCalls(provider, model, status, from, to, pageable);

        return ResponseEntity.ok(calls);
    }
}
