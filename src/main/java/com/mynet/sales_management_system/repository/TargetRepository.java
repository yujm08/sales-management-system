// TargetRepository.java - 목표 데이터 접근
package com.mynet.sales_management_system.repository;

import com.mynet.sales_management_system.entity.Target;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 목표 데이터 접근 레포지토리
 * - 회사별, 제품별, 기간별 목표 조회
 * - 전체 목표와 개별 목표 관리
 * - 목표 달성률 계산용 데이터 조회
 */
@Repository
public interface TargetRepository extends JpaRepository<Target, Long> {

        /**
         * 특정 회사의 특정 월 목표 조회
         */
        List<Target> findByCompanyIdAndTargetYearAndTargetMonth(Long companyId, Integer targetYear,
                        Integer targetMonth);

        /**
         * 전체 목표 조회 (company_id가 NULL)
         */
        @Query("SELECT t FROM Target t WHERE t.company IS NULL AND t.targetYear = :year AND t.targetMonth = :month")
        List<Target> findGlobalTargetsByYearAndMonth(@Param("year") Integer year, @Param("month") Integer month);

        /**
         * 특정 제품의 전체 목표 조회
         */
        @Query("SELECT t FROM Target t WHERE t.company IS NULL AND t.product.id = :productId " +
                        "AND t.targetYear = :year AND t.targetMonth = :month")
        Optional<Target> findGlobalTargetByProductAndYearAndMonth(@Param("productId") Long productId,
                        @Param("year") Integer year,
                        @Param("month") Integer month);

        /**
         * 특정 회사, 제품의 목표 조회
         */
        Optional<Target> findByCompanyIdAndProductIdAndTargetYearAndTargetMonth(
                        Long companyId, Long productId, Integer targetYear, Integer targetMonth);

        // 위의 메서드와 기능은 같은데, 이름과 파라미터 타입(int vs Integer)만 다름
        @Query("SELECT t FROM Target t WHERE t.company.id = :companyId AND t.product.id = :productId " +
                        "AND t.targetYear = :year AND t.targetMonth = :month")
        Optional<Target> findByCompanyIdAndProductIdAndYearAndMonth(@Param("companyId") Long companyId,
                        @Param("productId") Long productId,
                        @Param("year") int year,
                        @Param("month") int month);

        /**
         * 특정 월의 모든 회사별 목표 합계 조회 (전체 목표 검증용)
         */
        @Query("SELECT t.product.id, SUM(t.targetQuantity) " +
                        "FROM Target t WHERE t.company IS NOT NULL " +
                        "AND t.targetYear = :year AND t.targetMonth = :month " +
                        "GROUP BY t.product.id")
        List<Object[]> findCompanyTargetSumsByYearAndMonth(@Param("year") Integer year, @Param("month") Integer month);

        /**
         * 특정 제품의 연월별 목표 조회
         */
        @Query("SELECT t FROM Target t WHERE t.product.id = :productId " +
                        "AND t.targetYear = :year AND t.targetMonth = :month " +
                        "AND t.company IS NULL") // 전체 목표만 조회
        Optional<Target> findByProductIdAndYearAndMonth(@Param("productId") Long productId,
                        @Param("year") int year,
                        @Param("month") int month);

        /**
         * 특정 제품의 모든 회사별 목표 조회 (전체 목표 계산용)
         */
        @Query("SELECT t FROM Target t WHERE t.company IS NOT NULL " +
                        "AND t.product.id = :productId " +
                        "AND t.targetYear = :year AND t.targetMonth = :month")
        List<Target> findAllCompanyTargetsByProductAndYearAndMonth(
                        @Param("productId") Long productId,
                        @Param("year") Integer year,
                        @Param("month") Integer month);
}
