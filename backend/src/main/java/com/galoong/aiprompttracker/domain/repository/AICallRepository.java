package com.galoong.aiprompttracker.domain.repository;

import com.galoong.aiprompttracker.domain.entity.AICallRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 호출 기록 리포지토리
 */
@Repository
public interface AICallRepository extends JpaRepository<AICallRecord, Long> {

    /**
     * Provider별 호출 기록 조회
     */
    List<AICallRecord> findByProviderNameOrderByCreatedAtDesc(String providerName);

    /**
     * 함수명으로 호출 기록 조회
     */
    List<AICallRecord> findByFunctionNameOrderByCreatedAtDesc(String functionName);

    /**
     * 기간별 호출 기록 조회
     */
    List<AICallRecord> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Provider와 모델별 호출 기록 조회
     */
    List<AICallRecord> findByProviderNameAndModelNameOrderByCreatedAtDesc(
            String providerName, String modelName);

    /**
     * 카테고리별 호출 기록 조회
     */
    List<AICallRecord> findByCategoryOrderByCreatedAtDesc(String category);

    /**
     * 함수별 총 비용 집계
     */
    @Query("SELECT r.functionName, SUM(r.estimatedCost) " +
           "FROM AICallRecord r " +
           "WHERE r.createdAt >= :startDate " +
           "GROUP BY r.functionName " +
           "ORDER BY SUM(r.estimatedCost) DESC")
    List<Object[]> findTotalCostByFunction(@Param("startDate") LocalDateTime startDate);

    /**
     * Provider별 총 비용 집계
     */
    @Query("SELECT r.providerName, SUM(r.estimatedCost) " +
           "FROM AICallRecord r " +
           "WHERE r.createdAt >= :startDate " +
           "GROUP BY r.providerName " +
           "ORDER BY SUM(r.estimatedCost) DESC")
    List<Object[]> findTotalCostByProvider(@Param("startDate") LocalDateTime startDate);

    /**
     * 함수별 호출 횟수 집계
     */
    @Query("SELECT r.functionName, COUNT(r) " +
           "FROM AICallRecord r " +
           "WHERE r.createdAt >= :startDate " +
           "GROUP BY r.functionName " +
           "ORDER BY COUNT(r) DESC")
    List<Object[]> findCallCountByFunction(@Param("startDate") LocalDateTime startDate);

    /**
     * 함수별 평균 응답 시간
     */
    @Query("SELECT r.functionName, AVG(r.responseTimeMs) " +
           "FROM AICallRecord r " +
           "WHERE r.createdAt >= :startDate AND r.responseTimeMs IS NOT NULL " +
           "GROUP BY r.functionName")
    List<Object[]> findAvgResponseTimeByFunction(@Param("startDate") LocalDateTime startDate);

    /**
     * 성공률 조회
     */
    @Query("SELECT r.functionName, " +
           "SUM(CASE WHEN r.success = true THEN 1 ELSE 0 END) * 100.0 / COUNT(r) " +
           "FROM AICallRecord r " +
           "WHERE r.createdAt >= :startDate " +
           "GROUP BY r.functionName")
    List<Object[]> findSuccessRateByFunction(@Param("startDate") LocalDateTime startDate);

    /**
     * 최근 에러 기록 조회
     */
    List<AICallRecord> findTop10BySuccessFalseOrderByCreatedAtDesc();
}
