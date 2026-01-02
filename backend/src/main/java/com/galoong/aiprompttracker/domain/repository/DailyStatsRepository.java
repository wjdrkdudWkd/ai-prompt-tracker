package com.galoong.aiprompttracker.domain.repository;

import com.galoong.aiprompttracker.domain.entity.DailyStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 일별 통계 리포지토리
 */
@Repository
public interface DailyStatsRepository extends JpaRepository<DailyStats, Long> {

    /**
     * 특정 날짜의 통계 조회
     */
    List<DailyStats> findByStatsDateOrderByTotalCostDesc(LocalDate statsDate);

    /**
     * 기간별 통계 조회
     */
    List<DailyStats> findByStatsDateBetweenOrderByStatsDateDesc(
            LocalDate startDate, LocalDate endDate);

    /**
     * Provider별 기간 통계
     */
    List<DailyStats> findByProviderNameAndStatsDateBetweenOrderByStatsDateDesc(
            String providerName, LocalDate startDate, LocalDate endDate);

    /**
     * 함수별 기간 통계
     */
    List<DailyStats> findByFunctionNameAndStatsDateBetweenOrderByStatsDateDesc(
            String functionName, LocalDate startDate, LocalDate endDate);

    /**
     * 특정 날짜의 특정 함수 통계 조회
     */
    Optional<DailyStats> findByProviderNameAndModelNameAndFunctionNameAndStatsDate(
            String providerName, String modelName, String functionName, LocalDate statsDate);

    /**
     * 기간별 총 비용 집계
     */
    @Query("SELECT SUM(d.totalCost) FROM DailyStats d " +
           "WHERE d.statsDate BETWEEN :startDate AND :endDate")
    Double findTotalCostByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 기간별 총 호출 횟수 집계
     */
    @Query("SELECT SUM(d.totalCalls) FROM DailyStats d " +
           "WHERE d.statsDate BETWEEN :startDate AND :endDate")
    Long findTotalCallsByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Provider별 비용 비교
     */
    @Query("SELECT d.providerName, SUM(d.totalCost) FROM DailyStats d " +
           "WHERE d.statsDate BETWEEN :startDate AND :endDate " +
           "GROUP BY d.providerName " +
           "ORDER BY SUM(d.totalCost) DESC")
    List<Object[]> compareProviderCosts(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 상위 비용 함수 조회
     */
    @Query("SELECT d.functionName, SUM(d.totalCost) FROM DailyStats d " +
           "WHERE d.statsDate BETWEEN :startDate AND :endDate " +
           "GROUP BY d.functionName " +
           "ORDER BY SUM(d.totalCost) DESC")
    List<Object[]> findTopCostFunctions(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
