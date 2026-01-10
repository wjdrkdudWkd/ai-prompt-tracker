package com.galoong.aiprompttracker.api.service;

import com.galoong.aiprompttracker.api.config.ApiAutoConfiguration;
import com.galoong.aiprompttracker.api.dto.DashboardSummaryResponse;
import com.galoong.aiprompttracker.domain.repository.CallRepository;
import com.galoong.aiprompttracker.domain.repository.ExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Service for dashboard statistics
 */
@Slf4j
// @Service removed - registered as bean in ApiAutoConfiguration
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final ExecutionRepository executionRepository;
    private final CallRepository callRepository;
    private final ApiAutoConfiguration.PersistenceGuard persistenceGuard;

    /**
     * Get dashboard summary statistics
     */
    public DashboardSummaryResponse getSummary(Instant from, Instant to, String environment) {
        persistenceGuard.requirePersistence();
        // Build query based on filters
        Long totalExecutions;
        Long errorExecutions;
        Double totalCost;

        if (from != null && to != null && environment != null) {
            totalExecutions = executionRepository.countByStartedAtBetweenAndEnvironment(from, to, environment);
            errorExecutions = executionRepository.countByStartedAtBetweenAndEnvironmentAndStatus(from, to, environment, "error");
            totalCost = executionRepository.sumTotalCostByStartedAtBetweenAndEnvironment(from, to, environment);
        } else if (from != null && to != null) {
            totalExecutions = executionRepository.countByStartedAtBetween(from, to);
            errorExecutions = executionRepository.countByStartedAtBetweenAndStatus(from, to, "error");
            totalCost = executionRepository.sumTotalCostByStartedAtBetween(from, to);
        } else if (environment != null) {
            totalExecutions = executionRepository.countByEnvironment(environment);
            errorExecutions = executionRepository.countByEnvironmentAndStatus(environment, "error");
            totalCost = executionRepository.sumTotalCostByEnvironment(environment);
        } else {
            totalExecutions = executionRepository.count();
            errorExecutions = executionRepository.countByStatus("error");
            totalCost = executionRepository.sumTotalCost();
        }

        // Calculate execution error rate
        Double executionErrorRate = totalExecutions > 0
                ? (errorExecutions.doubleValue() / totalExecutions.doubleValue())
                : 0.0;

        // Get call-level stats
        Long totalCalls;
        Long errorCalls;
        Double avgLatencyMs;

        if (from != null && to != null && environment != null) {
            totalCalls = callRepository.countByExecutionEnvironmentAndCreatedAtBetween(environment, from, to);
            errorCalls = callRepository.countByExecutionEnvironmentAndCreatedAtBetweenAndStatus(environment, from, to, "error");
            avgLatencyMs = callRepository.avgLatencyMsByExecutionEnvironmentAndCreatedAtBetween(environment, from, to);
        } else if (from != null && to != null) {
            totalCalls = callRepository.countByCreatedAtBetween(from, to);
            errorCalls = callRepository.countByCreatedAtBetweenAndStatus(from, to, "error");
            avgLatencyMs = callRepository.avgLatencyMsByCreatedAtBetween(from, to);
        } else if (environment != null) {
            totalCalls = callRepository.countByExecutionEnvironment(environment);
            errorCalls = callRepository.countByExecutionEnvironmentAndStatus(environment, "error");
            avgLatencyMs = callRepository.avgLatencyMsByExecutionEnvironment(environment);
        } else {
            totalCalls = callRepository.count();
            errorCalls = callRepository.countByStatus("error");
            avgLatencyMs = callRepository.avgLatencyMs();
        }

        // Calculate call error rate
        Double callErrorRate = totalCalls > 0
                ? (errorCalls.doubleValue() / totalCalls.doubleValue())
                : 0.0;

        // Calculate average calls per execution
        Double avgCallsPerExecution = totalExecutions > 0
                ? (totalCalls.doubleValue() / totalExecutions.doubleValue())
                : 0.0;

        return DashboardSummaryResponse.builder()
                .totalCost(totalCost != null ? totalCost : 0.0)
                .totalExecutions(totalExecutions)
                .totalCalls(totalCalls)
                .avgLatencyMs(avgLatencyMs != null ? avgLatencyMs : 0.0)
                .avgCallsPerExecution(avgCallsPerExecution)
                .executionErrorRate(executionErrorRate)
                .callErrorRate(callErrorRate)
                .build();
    }
}
