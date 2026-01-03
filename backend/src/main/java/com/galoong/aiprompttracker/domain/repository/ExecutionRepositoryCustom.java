package com.galoong.aiprompttracker.domain.repository;

import com.galoong.aiprompttracker.api.dto.FunctionAggregateResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

/**
 * Custom repository methods for complex queries
 */
public interface ExecutionRepositoryCustom {

    /**
     * Get function aggregates with pagination and filtering
     */
    Page<FunctionAggregateResponse> findFunctionAggregates(
            Instant from,
            Instant to,
            String environment,
            String category,
            String status,
            String functionNameSearch,
            Pageable pageable
    );
}
