package com.galoong.aiprompttracker.domain.repository;

import com.galoong.aiprompttracker.domain.entity.CallRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Extended repository for CallRecord with query methods for dashboard APIs
 */
@Repository
public interface CallRepositoryExtended extends JpaRepository<CallRecord, String> {

    // Dashboard summary queries
    @Query("SELECT COUNT(c) FROM CallRecord c WHERE c.createdAt BETWEEN :from AND :to")
    Long countByCreatedAtBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT COUNT(c) FROM CallRecord c JOIN ExecutionRecord e ON c.executionId = e.id " +
           "WHERE e.environment = :env AND c.createdAt BETWEEN :from AND :to")
    Long countByExecutionEnvironmentAndCreatedAtBetween(@Param("env") String environment, @Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT COUNT(c) FROM CallRecord c WHERE c.createdAt BETWEEN :from AND :to AND c.status = :status")
    Long countByCreatedAtBetweenAndStatus(@Param("from") Instant from, @Param("to") Instant to, @Param("status") String status);

    @Query("SELECT COUNT(c) FROM CallRecord c JOIN ExecutionRecord e ON c.executionId = e.id " +
           "WHERE e.environment = :env AND c.createdAt BETWEEN :from AND :to AND c.status = :status")
    Long countByExecutionEnvironmentAndCreatedAtBetweenAndStatus(@Param("env") String environment, @Param("from") Instant from, @Param("to") Instant to, @Param("status") String status);

    @Query("SELECT COUNT(c) FROM CallRecord c JOIN ExecutionRecord e ON c.executionId = e.id WHERE e.environment = :env")
    Long countByExecutionEnvironment(@Param("env") String environment);

    @Query("SELECT COUNT(c) FROM CallRecord c JOIN ExecutionRecord e ON c.executionId = e.id " +
           "WHERE e.environment = :env AND c.status = :status")
    Long countByExecutionEnvironmentAndStatus(@Param("env") String environment, @Param("status") String status);

    @Query("SELECT COUNT(c) FROM CallRecord c WHERE c.status = :status")
    Long countByStatus(@Param("status") String status);

    @Query("SELECT COALESCE(AVG(c.latencyMs), 0.0) FROM CallRecord c WHERE c.createdAt BETWEEN :from AND :to")
    Double avgLatencyMsByCreatedAtBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT COALESCE(AVG(c.latencyMs), 0.0) FROM CallRecord c JOIN ExecutionRecord e ON c.executionId = e.id " +
           "WHERE e.environment = :env AND c.createdAt BETWEEN :from AND :to")
    Double avgLatencyMsByExecutionEnvironmentAndCreatedAtBetween(@Param("env") String environment, @Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT COALESCE(AVG(c.latencyMs), 0.0) FROM CallRecord c JOIN ExecutionRecord e ON c.executionId = e.id WHERE e.environment = :env")
    Double avgLatencyMsByExecutionEnvironment(@Param("env") String environment);

    @Query("SELECT COALESCE(AVG(c.latencyMs), 0.0) FROM CallRecord c")
    Double avgLatencyMs();

    // Call queries
    List<CallRecord> findByExecutionIdOrderByCreatedAtAsc(String executionId);

    @Query("SELECT c FROM CallRecord c JOIN ExecutionRecord e ON c.executionId = e.id " +
           "WHERE e.functionName = :functionName ORDER BY c.createdAt DESC")
    Page<CallRecord> findByFunctionName(@Param("functionName") String functionName, Pageable pageable);

    // Provider/Model breakdown
    @Query("SELECT c.provider, COUNT(c), COALESCE(SUM(c.cost), 0.0) FROM CallRecord c " +
           "JOIN ExecutionRecord e ON c.executionId = e.id " +
           "WHERE e.functionName = :functionName " +
           "GROUP BY c.provider ORDER BY COUNT(c) DESC")
    List<Object[]> findProviderBreakdownByFunction(@Param("functionName") String functionName);

    @Query("SELECT c.model, COUNT(c), COALESCE(SUM(c.cost), 0.0) FROM CallRecord c " +
           "JOIN ExecutionRecord e ON c.executionId = e.id " +
           "WHERE e.functionName = :functionName " +
           "GROUP BY c.model ORDER BY COUNT(c) DESC")
    List<Object[]> findModelBreakdownByFunction(@Param("functionName") String functionName);

    // Search with filters
    Page<CallRecord> findByProviderAndCreatedAtBetween(String provider, Instant from, Instant to, Pageable pageable);

    Page<CallRecord> findByModelAndCreatedAtBetween(String model, Instant from, Instant to, Pageable pageable);

    Page<CallRecord> findByStatusAndCreatedAtBetween(String status, Instant from, Instant to, Pageable pageable);
}
