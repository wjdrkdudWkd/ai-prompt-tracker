package com.galoong.aiprompttracker.domain.repository;

import com.galoong.aiprompttracker.domain.entity.CallRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for CallRecord entities
 */
@Repository
public interface CallRepository extends JpaRepository<CallRecord, String> {

    /**
     * Find calls by execution ID
     */
    List<CallRecord> findByExecutionIdOrderByCreatedAtAsc(String executionId);

    /**
     * Find calls by provider
     */
    List<CallRecord> findByProviderOrderByCreatedAtDesc(String provider);

    /**
     * Find calls by provider and model
     */
    List<CallRecord> findByProviderAndModelOrderByCreatedAtDesc(String provider, String model);

    /**
     * Find calls by time range
     */
    List<CallRecord> findByCreatedAtBetweenOrderByCreatedAtDesc(
            Instant startTime, Instant endTime);

    /**
     * Find recent calls
     */
    List<CallRecord> findTop20ByOrderByCreatedAtDesc();

    /**
     * Get total cost by provider
     */
    @Query("SELECT c.provider, SUM(c.cost) " +
           "FROM CallRecord c " +
           "WHERE c.createdAt >= :startTime AND c.cost IS NOT NULL " +
           "GROUP BY c.provider " +
           "ORDER BY SUM(c.cost) DESC")
    List<Object[]> findTotalCostByProvider(@Param("startTime") Instant startTime);

    /**
     * Get total cost by model
     */
    @Query("SELECT c.provider, c.model, SUM(c.cost) " +
           "FROM CallRecord c " +
           "WHERE c.createdAt >= :startTime AND c.cost IS NOT NULL " +
           "GROUP BY c.provider, c.model " +
           "ORDER BY SUM(c.cost) DESC")
    List<Object[]> findTotalCostByModel(@Param("startTime") Instant startTime);

    /**
     * Get call count by provider
     */
    @Query("SELECT c.provider, COUNT(c) " +
           "FROM CallRecord c " +
           "WHERE c.createdAt >= :startTime " +
           "GROUP BY c.provider " +
           "ORDER BY COUNT(c) DESC")
    List<Object[]> findCallCountByProvider(@Param("startTime") Instant startTime);

    /**
     * Get average latency by provider
     */
    @Query("SELECT c.provider, AVG(c.latencyMs) " +
           "FROM CallRecord c " +
           "WHERE c.createdAt >= :startTime " +
           "GROUP BY c.provider")
    List<Object[]> findAvgLatencyByProvider(@Param("startTime") Instant startTime);

    /**
     * Get token usage by provider
     */
    @Query("SELECT c.provider, SUM(c.totalTokens) " +
           "FROM CallRecord c " +
           "WHERE c.createdAt >= :startTime AND c.totalTokens IS NOT NULL " +
           "GROUP BY c.provider " +
           "ORDER BY SUM(c.totalTokens) DESC")
    List<Object[]> findTotalTokensByProvider(@Param("startTime") Instant startTime);

    /**
     * Find recent failed calls
     */
    List<CallRecord> findTop10ByStatusOrderByCreatedAtDesc(String status);
}
