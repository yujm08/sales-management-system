// DailySalesRepository.java - 일별 실적 데이터 접근
package com.mynet.sales_management_system.repository;

import com.mynet.sales_management_system.entity.DailySales;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 일별 실적 데이터 접근 레포지토리
 * - 날짜별, 회사별, 제품별 실적 조회
 * - 통계 집계용 쿼리
 * - 기간별 데이터 조회
 */
@Repository
public interface DailySalesRepository extends JpaRepository<DailySales, Long> {

  List<DailySales> findByCompanyIdAndSalesDate(Long companyId, LocalDate salesDate);

  /**
   * 특정 회사의 특정 날짜 데이터 조회
   */
  List<DailySales> findByCompanyIdAndSalesDateOrderByProduct_CategoryAscProduct_ProductCodeAsc(
      Long companyId, LocalDate salesDate);

  /**
   * 특정 회사, 제품, 날짜의 실적 조회
   */
  @Query("""
      SELECT d FROM DailySales d
      WHERE d.company.id = :companyId
        AND d.product.id = :productId
        AND d.salesDate = :salesDate
      """)
  Optional<DailySales> findByCompanyIdAndProductIdAndSalesDate(Long companyId, Long productId,
      LocalDate salesDate);

  /**
   * 특정 회사의 월별 실적 조회
   */
  @Query("SELECT d FROM DailySales d WHERE d.company.id = :companyId " +
      "AND YEAR(d.salesDate) = :year AND MONTH(d.salesDate) = :month " +
      "ORDER BY d.product.category ASC, d.product.productCode ASC, d.salesDate ASC")
  List<DailySales> findByCompanyIdAndYearAndMonth(@Param("companyId") Long companyId,
      @Param("year") int year,
      @Param("month") int month);

  /**
   * 전체 회사의 특정 날짜 데이터 조회 (마이넷 조회용)
   */
  @Query("SELECT d FROM DailySales d WHERE d.salesDate = :salesDate " +
      "ORDER BY d.company.name ASC, d.product.category ASC, d.product.productCode ASC")
  List<DailySales> findBySalesDateOrderByCompanyAndProduct(@Param("salesDate") LocalDate salesDate);

  /**
   * 전체 회사의 특정 년월 데이터 조회 (마이넷 년도별 비교용)
   */
  @Query("SELECT d FROM DailySales d WHERE YEAR(d.salesDate) = :year " +
      "AND MONTH(d.salesDate) = :month " +
      "ORDER BY d.salesDate ASC")
  List<DailySales> findAllByYearAndMonth(@Param("year") int year,
      @Param("month") int month);

  /**
   * 특정 제품의 기간별 데이터 조회 (기간별 비교용)
   */
  @Query("SELECT d FROM DailySales d WHERE d.product.id = :productId " +
      "AND d.salesDate BETWEEN :startDate AND :endDate " +
      "ORDER BY d.salesDate ASC, d.company.name ASC")
  List<DailySales> findByProductIdAndDateRange(@Param("productId") Long productId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  /**
   * 전체 제품의 기간별 데이터 조회
   */
  @Query("SELECT d FROM DailySales d WHERE d.salesDate BETWEEN :startDate AND :endDate " +
      "ORDER BY d.salesDate ASC, d.product.category ASC, d.product.productCode ASC")
  List<DailySales> findByDateRangeOrderByDateAndProduct(@Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  /**
   * 월별 집계 조회 (통계용)
   */
  @Query("SELECT d.product.id, d.product.productName, d.product.category, " +
      "SUM(d.quantity) as totalQuantity " +
      "FROM DailySales d WHERE d.company.id = :companyId " +
      "AND YEAR(d.salesDate) = :year AND MONTH(d.salesDate) = :month " +
      "GROUP BY d.product.id, d.product.productName, d.product.category " +
      "ORDER BY d.product.category ASC, d.product.productCode ASC")
  List<Object[]> findMonthlySummaryByCompanyId(@Param("companyId") Long companyId,
      @Param("year") int year,
      @Param("month") int month);

  /**
   * 특정 제품의 특정 날짜 전체 회사 데이터 조회
   */
  @Query("SELECT d FROM DailySales d WHERE d.product.id = :productId AND d.salesDate = :salesDate " +
      "ORDER BY d.company.name ASC")
  List<DailySales> findByProductIdAndSalesDate(@Param("productId") Long productId,
      @Param("salesDate") LocalDate salesDate);

  /**
   * 특정 제품의 월별 데이터 조회 (선택된 날짜까지)
   */
  @Query("SELECT d FROM DailySales d WHERE d.product.id = :productId " +
      "AND YEAR(d.salesDate) = :year AND MONTH(d.salesDate) = :month " +
      "AND DAY(d.salesDate) <= :day " +
      "ORDER BY d.salesDate ASC, d.company.name ASC")
  List<DailySales> findByProductIdAndYearAndMonth(@Param("productId") Long productId,
      @Param("year") int year,
      @Param("month") int month,
      @Param("day") int day);

  /**
   * 특정 회사, 제품의 월별 데이터 조회 (선택된 날짜까지)
   */
  @Query("SELECT d FROM DailySales d WHERE d.company.id = :companyId AND d.product.id = :productId " +
      "AND YEAR(d.salesDate) = :year AND MONTH(d.salesDate) = :month " +
      "AND DAY(d.salesDate) <= :day " +
      "ORDER BY d.salesDate ASC")
  List<DailySales> findByCompanyIdAndProductIdAndYearAndMonth(@Param("companyId") Long companyId,
      @Param("productId") Long productId,
      @Param("year") int year,
      @Param("month") int month,
      @Param("day") int day);

  /**
   * 특정 제품의 특정 년월 전체 회사 데이터 조회 (년도별 비교용)
   */
  @Query("SELECT d FROM DailySales d WHERE d.product.id = :productId " +
      "AND YEAR(d.salesDate) = :year AND MONTH(d.salesDate) = :month")
  List<DailySales> findByProductIdAndYearAndMonthForComparison(@Param("productId") Long productId,
      @Param("year") int year,
      @Param("month") int month);

  /**
   * 특정 회사, 제품의 기간별 데이터 조회 - (날짜 범위로 정확하게 조회)
   */
  @Query("SELECT d FROM DailySales d WHERE d.company.id = :companyId AND d.product.id = :productId " +
      "AND d.salesDate >= :startDate AND d.salesDate <= :endDate " +
      "ORDER BY d.salesDate ASC")
  List<DailySales> findByCompanyIdAndProductIdBetweenDates(@Param("companyId") Long companyId,
      @Param("productId") Long productId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  /**
   * 특정 제품의 기간별 데이터 조회 (날짜 범위 제한)
   */
  @Query("SELECT d FROM DailySales d WHERE d.product.id = :productId " +
      "AND d.salesDate >= :startDate AND d.salesDate <= :endDate " +
      "ORDER BY d.salesDate ASC")
  List<DailySales> findByProductIdBetweenDates(@Param("productId") Long productId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  /**
   * 네이티브 쿼리: 특정 회사, 제품, 날짜의 실적 조회
   */
  @Query(value = """
      SELECT * FROM daily_sales
      WHERE company_id = :companyId
        AND product_id = :productId
        AND sales_date = CAST(:salesDate AS DATE)
      """, nativeQuery = true)
  Optional<DailySales> findByCompanyIdAndProductIdAndSalesDateNative(
      @Param("companyId") Long companyId,
      @Param("productId") Long productId,
      @Param("salesDate") LocalDate salesDate);

}
