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
 * Unified repository for CallRecord entities.
 *
 * <p>Consolidates all query methods from base and extended repositories
 * to eliminate fragmentation and simplify dependency injection.
 */
@Repository
public interface CallRepository extends JpaRepository<CallRecord, String> {

    // ========== Basic Queries ==========

    List<CallRecord> findByExecutionIdOrderByCreatedAtAsc(String executionId);
    List<CallRecord> findByProviderOrderByCreatedAtDesc(String provider);
    List<CallRecord> findByProviderAndModelOrderByCreatedAtDesc(String provider, String model);
    List<CallRecord> findByCreatedAtBetweenOrderByCreatedAtDesc(Instant startTime, Instant endTime);
    List<CallRecord> findTop20ByOrderByCreatedAtDesc();
    List<CallRecord> findTop10ByStatusOrderByCreatedAtDesc(String status);

    // ========== Dashboard Summary Queries (consolidated from Extended) ==========

    @Query("SELECT COUNT(c) FROM CallRecord c WHERE c.createdAt BETWEEN :from AND :to")
    Long countByCreatedAtBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT COUNT(c) FROM CallRecord c JOIN ExecutionRecord e ON c.executionId = e.id WHERE e.environment = :env AND c.createdAt BETWEEN :from AND :to")
    Long countByExecutionEnvironmentAndCreatedAtBetween(@Param("env") String environment, @Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT COUNT(c) FROM CallRecord c WHERE c.createdAt BETWEEN :from AND :to AND c.status = :status")
    Long countByCreatedAtBetweenAndStatus(@Param("from") Instant from, @Param("to") Instant to, @Param("status") String status);

    @Query("SELECT COUNT(c) FROM CallRecord c JOIN ExecutionRecord e ON c.executionId = e.id WHERE e.environment = :env AND c.createdAt BETWEEN :from AND :to AND c.status = :status")
    Long countByExecutionEnvironmentAndCreatedAtBetweenAndStatus(@Param("env") String environment, @Param("from") Instant from, @Param("to") Instant to, @Param("status") String status);

    @Query("SELECT COUNT(c) FROM CallRecord c JOIN ExecutionRecord e ON c.executionId = e.id WHERE e.environment = :env")
    Long countByExecutionEnvironment(@Param("env") String environment);

    @Query("SELECT COUNT(c) FROM CallRecord c JOIN ExecutionRecord e ON c.executionId = e.id WHERE e.environment = :env AND c.status = :status")
    Long countByExecutionEnvironmentAndStatus(@Param("env") String environment, @Param("status") String status);

    @Query("SELECT COUNT(c) FROM CallRecord c WHERE c.status = :status")
    Long countByStatus(@Param("status") String status);

    @Query("SELECT COALESCE(AVG(c.latencyMs), 0.0) FROM CallRecord c WHERE c.createdAt BETWEEN :from AND :to")
    Double avgLatencyMsByCreatedAtBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT COALESCE(AVG(c.latencyMs), 0.0) FROM CallRecord c JOIN ExecutionRecord e ON c.executionId = e.id WHERE e.environment = :env AND c.createdAt BETWEEN :from AND :to")
    Double avgLatencyMsByExecutionEnvironmentAndCreatedAtBetween(@Param("env") String environment, @Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT COALESCE(AVG(c.latencyMs), 0.0) FROM CallRecord c JOIN ExecutionRecord e ON c.executionId = e.id WHERE e.environment = :env")
    Double avgLatencyMsByExecutionEnvironment(@Param("env") String environment);

    @Query("SELECT COALESCE(AVG(c.latencyMs), 0.0) FROM CallRecord c")
    Double avgLatencyMs();

    // ========== Paginated Queries (consolidated from Extended) ==========

    @Query("SELECT c FROM CallRecord c JOIN ExecutionRecord e ON c.executionId = e.id WHERE e.functionName = :functionName ORDER BY c.createdAt DESC")
    Page<CallRecord> findByFunctionName(@Param("functionName") String functionName, Pageable pageable);

    Page<CallRecord> findByProviderAndCreatedAtBetween(String provider, Instant from, Instant to, Pageable pageable);
    Page<CallRecord> findByModelAndCreatedAtBetween(String model, Instant from, Instant to, Pageable pageable);
    Page<CallRecord> findByStatusAndCreatedAtBetween(String status, Instant from, Instant to, Pageable pageable);

    // ========== Analytics Queries ==========

    @Query("SELECT c.provider, SUM(c.cost) FROM CallRecord c WHERE c.createdAt >= :startTime AND c.cost IS NOT NULL GROUP BY c.provider ORDER BY SUM(c.cost) DESC")
    List<Object[]> findTotalCostByProvider(@Param("startTime") Instant startTime);

    @Query("SELECT c.provider, c.model, SUM(c.cost) FROM CallRecord c WHERE c.createdAt >= :startTime AND c.cost IS NOT NULL GROUP BY c.provider, c.model ORDER BY SUM(c.cost) DESC")
    List<Object[]> findTotalCostByModel(@Param("startTime") Instant startTime);

    @Query("SELECT c.provider, COUNT(c) FROM CallRecord c WHERE c.createdAt >= :startTime GROUP BY c.provider ORDER BY COUNT(c) DESC")
    List<Object[]> findCallCountByProvider(@Param("startTime") Instant startTime);

    @Query("SELECT c.provider, AVG(c.latencyMs) FROM CallRecord c WHERE c.createdAt >= :startTime GROUP BY c.provider")
    List<Object[]> findAvgLatencyByProvider(@Param("startTime") Instant startTime);

    @Query("SELECT c.provider, SUM(c.totalTokens) FROM CallRecord c WHERE c.createdAt >= :startTime AND c.totalTokens IS NOT NULL GROUP BY c.provider ORDER BY SUM(c.totalTokens) DESC")
    List<Object[]> findTotalTokensByProvider(@Param("startTime") Instant startTime);

    @Query("SELECT c.provider, COUNT(c), COALESCE(SUM(c.cost), 0.0) FROM CallRecord c JOIN ExecutionRecord e ON c.executionId = e.id WHERE e.functionName = :functionName GROUP BY c.provider ORDER BY COUNT(c) DESC")
    List<Object[]> findProviderBreakdownByFunction(@Param("functionName") String functionName);

    @Query("SELECT c.model, COUNT(c), COALESCE(SUM(c.cost), 0.0) FROM CallRecord c JOIN ExecutionRecord e ON c.executionId = e.id WHERE e.functionName = :functionName GROUP BY c.model ORDER BY COUNT(c) DESC")
    List<Object[]> findModelBreakdownByFunction(@Param("functionName") String functionName);
}
