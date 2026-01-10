package com.galoong.aiprompttracker.domain.repository;

import com.galoong.aiprompttracker.domain.entity.ExecutionRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Unified repository for ExecutionRecord entities.
 *
 * <p>Consolidates all query methods from base and extended repositories
 * to eliminate fragmentation and simplify dependency injection.
 */
@Repository
public interface ExecutionRepository extends JpaRepository<ExecutionRecord, String> {

    // ========== Basic Queries ==========

    List<ExecutionRecord> findByFunctionNameOrderByStartedAtDesc(String functionName);
    List<ExecutionRecord> findByStartedAtBetweenOrderByStartedAtDesc(Instant startTime, Instant endTime);
    List<ExecutionRecord> findByEnvironmentOrderByStartedAtDesc(String environment);
    List<ExecutionRecord> findByStatusOrderByStartedAtDesc(String status);
    List<ExecutionRecord> findByCategoryOrderByStartedAtDesc(String category);
    List<ExecutionRecord> findTop10ByOrderByStartedAtDesc();
    List<ExecutionRecord> findTop10ByStatusOrderByStartedAtDesc(String status);

    // ========== Dashboard Summary Queries (consolidated from Extended) ==========

    @Query("SELECT COUNT(e) FROM ExecutionRecord e WHERE e.startedAt BETWEEN :from AND :to")
    Long countByStartedAtBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT COUNT(e) FROM ExecutionRecord e WHERE e.startedAt BETWEEN :from AND :to AND e.environment = :env")
    Long countByStartedAtBetweenAndEnvironment(@Param("from") Instant from, @Param("to") Instant to, @Param("env") String environment);

    @Query("SELECT COUNT(e) FROM ExecutionRecord e WHERE e.startedAt BETWEEN :from AND :to AND e.status = :status")
    Long countByStartedAtBetweenAndStatus(@Param("from") Instant from, @Param("to") Instant to, @Param("status") String status);

    @Query("SELECT COUNT(e) FROM ExecutionRecord e WHERE e.startedAt BETWEEN :from AND :to AND e.environment = :env AND e.status = :status")
    Long countByStartedAtBetweenAndEnvironmentAndStatus(@Param("from") Instant from, @Param("to") Instant to, @Param("env") String environment, @Param("status") String status);

    @Query("SELECT COUNT(e) FROM ExecutionRecord e WHERE e.environment = :env")
    Long countByEnvironment(@Param("env") String environment);

    @Query("SELECT COUNT(e) FROM ExecutionRecord e WHERE e.environment = :env AND e.status = :status")
    Long countByEnvironmentAndStatus(@Param("env") String environment, @Param("status") String status);

    @Query("SELECT COUNT(e) FROM ExecutionRecord e WHERE e.status = :status")
    Long countByStatus(@Param("status") String status);

    @Query("SELECT COALESCE(SUM(e.totalCost), 0.0) FROM ExecutionRecord e WHERE e.startedAt BETWEEN :from AND :to")
    Double sumTotalCostByStartedAtBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT COALESCE(SUM(e.totalCost), 0.0) FROM ExecutionRecord e WHERE e.startedAt BETWEEN :from AND :to AND e.environment = :env")
    Double sumTotalCostByStartedAtBetweenAndEnvironment(@Param("from") Instant from, @Param("to") Instant to, @Param("env") String environment);

    @Query("SELECT COALESCE(SUM(e.totalCost), 0.0) FROM ExecutionRecord e WHERE e.environment = :env")
    Double sumTotalCostByEnvironment(@Param("env") String environment);

    @Query("SELECT COALESCE(SUM(e.totalCost), 0.0) FROM ExecutionRecord e")
    Double sumTotalCost();

    // ========== Paginated & Advanced Queries (consolidated from Extended) ==========

    Page<ExecutionRecord> findByFunctionNameContainingIgnoreCaseOrderByStartedAtDesc(String functionName, Pageable pageable);
    Page<ExecutionRecord> findByFunctionNameAndStartedAtBetween(String functionName, Instant from, Instant to, Pageable pageable);
    Optional<ExecutionRecord> findFirstByFunctionNameOrderByStartedAtDesc(String functionName);

    // ========== Analytics Queries ==========

    @Query("SELECT e.functionName, SUM(e.totalCost) FROM ExecutionRecord e WHERE e.startedAt >= :startTime GROUP BY e.functionName ORDER BY SUM(e.totalCost) DESC")
    List<Object[]> findTotalCostByFunction(@Param("startTime") Instant startTime);

    @Query("SELECT e.functionName, COUNT(e) FROM ExecutionRecord e WHERE e.startedAt >= :startTime GROUP BY e.functionName ORDER BY COUNT(e) DESC")
    List<Object[]> findExecutionCountByFunction(@Param("startTime") Instant startTime);

    @Query("SELECT e.functionName, SUM(CASE WHEN e.status = 'success' THEN 1 ELSE 0 END) * 100.0 / COUNT(e) FROM ExecutionRecord e WHERE e.startedAt >= :startTime GROUP BY e.functionName")
    List<Object[]> findSuccessRateByFunction(@Param("startTime") Instant startTime);
}
