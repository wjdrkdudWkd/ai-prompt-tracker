package com.galoong.aiprompttracker.domain.repository;

import com.galoong.aiprompttracker.domain.entity.ExecutionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for ExecutionRecord entities
 */
@Repository
public interface ExecutionRepository extends JpaRepository<ExecutionRecord, String> {

    /**
     * Find executions by function name
     */
    List<ExecutionRecord> findByFunctionNameOrderByStartedAtDesc(String functionName);

    /**
     * Find executions by time range
     */
    List<ExecutionRecord> findByStartedAtBetweenOrderByStartedAtDesc(
            Instant startTime, Instant endTime);

    /**
     * Find executions by environment
     */
    List<ExecutionRecord> findByEnvironmentOrderByStartedAtDesc(String environment);

    /**
     * Find executions by status
     */
    List<ExecutionRecord> findByStatusOrderByStartedAtDesc(String status);

    /**
     * Find executions by category
     */
    List<ExecutionRecord> findByCategoryOrderByStartedAtDesc(String category);

    /**
     * Find recent executions
     */
    List<ExecutionRecord> findTop10ByOrderByStartedAtDesc();

    /**
     * Get total cost by function
     */
    @Query("SELECT e.functionName, SUM(e.totalCost) " +
           "FROM ExecutionRecord e " +
           "WHERE e.startedAt >= :startTime " +
           "GROUP BY e.functionName " +
           "ORDER BY SUM(e.totalCost) DESC")
    List<Object[]> findTotalCostByFunction(@Param("startTime") Instant startTime);

    /**
     * Get execution count by function
     */
    @Query("SELECT e.functionName, COUNT(e) " +
           "FROM ExecutionRecord e " +
           "WHERE e.startedAt >= :startTime " +
           "GROUP BY e.functionName " +
           "ORDER BY COUNT(e) DESC")
    List<Object[]> findExecutionCountByFunction(@Param("startTime") Instant startTime);

    /**
     * Get success rate by function
     */
    @Query("SELECT e.functionName, " +
           "SUM(CASE WHEN e.status = 'success' THEN 1 ELSE 0 END) * 100.0 / COUNT(e) " +
           "FROM ExecutionRecord e " +
           "WHERE e.startedAt >= :startTime " +
           "GROUP BY e.functionName")
    List<Object[]> findSuccessRateByFunction(@Param("startTime") Instant startTime);

    /**
     * Get recent failed executions
     */
    List<ExecutionRecord> findTop10ByStatusOrderByStartedAtDesc(String status);
}
