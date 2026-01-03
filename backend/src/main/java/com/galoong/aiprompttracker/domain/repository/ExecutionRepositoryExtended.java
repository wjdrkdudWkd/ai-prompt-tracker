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
 * Extended repository for ExecutionRecord with query methods for dashboard APIs
 */
@Repository
public interface ExecutionRepositoryExtended extends JpaRepository<ExecutionRecord, String> {

    // Dashboard summary queries
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

    // Function list queries
    Page<ExecutionRecord> findByFunctionNameContainingIgnoreCaseOrderByStartedAtDesc(String functionName, Pageable pageable);

    Page<ExecutionRecord> findByFunctionNameAndStartedAtBetween(String functionName, Instant from, Instant to, Pageable pageable);

    Optional<ExecutionRecord> findFirstByFunctionNameOrderByStartedAtDesc(String functionName);
}
