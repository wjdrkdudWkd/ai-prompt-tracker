package com.galoong.aiprompttracker.api.service;

import com.galoong.aiprompttracker.api.dto.ExecutionSummaryResponse;
import com.galoong.aiprompttracker.api.dto.FunctionAggregateResponse;
import com.galoong.aiprompttracker.api.dto.FunctionDetailResponse;
import com.galoong.aiprompttracker.domain.entity.ExecutionRecord;
import com.galoong.aiprompttracker.domain.repository.CallRepositoryExtended;
import com.galoong.aiprompttracker.domain.repository.ExecutionRepositoryExtended;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for function-level statistics and queries
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FunctionService {

    private final ExecutionRepositoryExtended executionRepository;
    private final CallRepositoryExtended callRepository;

    /**
     * Get function aggregates with pagination
     */
    public Page<FunctionAggregateResponse> getFunctionAggregates(
            Instant from,
            Instant to,
            String environment,
            String category,
            String status,
            String functionNameSearch,
            Pageable pageable) {

        // For MVP, we'll fetch all executions and aggregate in memory
        // For production, this should be done with native SQL or custom queries
        List<ExecutionRecord> executions = executionRepository.findAll();

        // Group by function name
        Map<String, List<ExecutionRecord>> grouped = executions.stream()
                .collect(Collectors.groupingBy(ExecutionRecord::getFunctionName));

        List<FunctionAggregateResponse> aggregates = grouped.entrySet().stream()
                .map(entry -> {
                    String functionName = entry.getKey();
                    List<ExecutionRecord> execs = entry.getValue();

                    long executionCount = execs.size();
                    long errorCount = execs.stream().filter(e -> "error".equals(e.getStatus())).count();
                    double errorRate = executionCount > 0 ? (double) errorCount / executionCount : 0.0;

                    double totalCost = execs.stream()
                            .filter(e -> e.getTotalCost() != null)
                            .mapToDouble(ExecutionRecord::getTotalCost)
                            .sum();

                    double avgDurationMs = execs.stream()
                            .filter(e -> e.getDurationMs() != null)
                            .mapToLong(ExecutionRecord::getDurationMs)
                            .average()
                            .orElse(0.0);

                    int totalCalls = execs.stream()
                            .filter(e -> e.getCallsCount() != null)
                            .mapToInt(ExecutionRecord::getCallsCount)
                            .sum();

                    double avgCallsPerExecution = executionCount > 0 ? (double) totalCalls / executionCount : 0.0;

                    // Get category and tags from latest execution
                    ExecutionRecord latest = execs.stream()
                            .max((e1, e2) -> e1.getStartedAt().compareTo(e2.getStartedAt()))
                            .orElse(execs.get(0));

                    return FunctionAggregateResponse.builder()
                            .functionName(functionName)
                            .category(latest.getCategory())
                            .tags(latest.getTags())
                            .executions(executionCount)
                            .calls((long) totalCalls)
                            .callsPerExecution(avgCallsPerExecution)
                            .totalCost(totalCost)
                            .avgExecutionTimeMs(avgDurationMs)
                            .errorRate(errorRate)
                            .build();
                })
                .sorted((a, b) -> Double.compare(b.getTotalCost(), a.getTotalCost())) // Sort by cost desc
                .collect(Collectors.toList());

        // Simple pagination in memory for MVP
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), aggregates.size());
        List<FunctionAggregateResponse> page = start < aggregates.size() ?
                aggregates.subList(start, end) : new ArrayList<>();

        return new PageImpl<>(page, pageable, aggregates.size());
    }

    /**
     * Get function detail with provider/model breakdown
     */
    public FunctionDetailResponse getFunctionDetail(String functionName) {
        // Get executions for this function
        List<ExecutionRecord> executions = executionRepository.findAll().stream()
                .filter(e -> functionName.equals(e.getFunctionName()))
                .collect(Collectors.toList());

        if (executions.isEmpty()) {
            return null;
        }

        long executionCount = executions.size();
        long errorCount = executions.stream().filter(e -> "error".equals(e.getStatus())).count();
        double errorRate = executionCount > 0 ? (double) errorCount / executionCount : 0.0;

        double totalCost = executions.stream()
                .filter(e -> e.getTotalCost() != null)
                .mapToDouble(ExecutionRecord::getTotalCost)
                .sum();

        int totalCalls = executions.stream()
                .filter(e -> e.getCallsCount() != null)
                .mapToInt(ExecutionRecord::getCallsCount)
                .sum();

        double avgDurationMs = executions.stream()
                .filter(e -> e.getDurationMs() != null)
                .mapToLong(ExecutionRecord::getDurationMs)
                .average()
                .orElse(0.0);

        ExecutionRecord latest = executions.stream()
                .max((e1, e2) -> e1.getStartedAt().compareTo(e2.getStartedAt()))
                .orElse(executions.get(0));

        // Get provider breakdown
        List<Object[]> providerBreakdown = callRepository.findProviderBreakdownByFunction(functionName);
        List<FunctionDetailResponse.ProviderBreakdown> providers = providerBreakdown.stream()
                .map(row -> FunctionDetailResponse.ProviderBreakdown.builder()
                        .provider((String) row[0])
                        .calls(((Number) row[1]).longValue())
                        .cost(((Number) row[2]).doubleValue())
                        .build())
                .limit(5)
                .collect(Collectors.toList());

        // Get model breakdown
        List<Object[]> modelBreakdown = callRepository.findModelBreakdownByFunction(functionName);
        List<FunctionDetailResponse.ModelBreakdown> models = modelBreakdown.stream()
                .map(row -> FunctionDetailResponse.ModelBreakdown.builder()
                        .model((String) row[0])
                        .calls(((Number) row[1]).longValue())
                        .cost(((Number) row[2]).doubleValue())
                        .build())
                .limit(5)
                .collect(Collectors.toList());

        return FunctionDetailResponse.builder()
                .functionName(functionName)
                .category(latest.getCategory())
                .tags(latest.getTags())
                .totalExecutions(executionCount)
                .totalCalls((long) totalCalls)
                .totalCost(totalCost)
                .avgExecutionTimeMs(avgDurationMs)
                .errorRate(errorRate)
                .topProviders(providers)
                .topModels(models)
                .build();
    }

    /**
     * Get execution history for a function
     */
    public Page<ExecutionSummaryResponse> getFunctionExecutions(
            String functionName,
            Instant from,
            Instant to,
            Pageable pageable) {

        Page<ExecutionRecord> executions;
        if (from != null && to != null) {
            executions = executionRepository.findByFunctionNameAndStartedAtBetween(
                    functionName, from, to, pageable);
        } else {
            executions = executionRepository.findByFunctionNameContainingIgnoreCaseOrderByStartedAtDesc(
                    functionName, pageable);
        }

        return executions.map(e -> ExecutionSummaryResponse.builder()
                .executionId(e.getId())
                .functionName(e.getFunctionName())
                .startedAt(e.getStartedAt())
                .finishedAt(e.getFinishedAt())
                .durationMs(e.getDurationMs())
                .callsCount(e.getCallsCount())
                .totalTokens(e.getTotalTokens())
                .totalCost(e.getTotalCost())
                .status(e.getStatus())
                .environment(e.getEnvironment())
                .build());
    }
}
