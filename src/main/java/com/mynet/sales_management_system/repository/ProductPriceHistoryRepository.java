// ProductPriceHistoryRepository.java - 제품 가격 이력 데이터 접근
package com.mynet.sales_management_system.repository;

import com.mynet.sales_management_system.entity.ProductPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 제품 가격 이력 데이터 접근 레포지토리
 * - 특정 시점의 가격 조회 (과거 데이터 계산용)
 * - 현재 적용 중인 가격 조회
 * - 가격 변경 이력 관리
 */
@Repository
public interface ProductPriceHistoryRepository extends JpaRepository<ProductPriceHistory, Long> {

    /**
     * 제품의 현재 적용 중인 가격 조회 (effective_to가 NULL)
     */
    @Query("SELECT p FROM ProductPriceHistory p WHERE p.product.id = :productId AND p.effectiveTo IS NULL")
    Optional<ProductPriceHistory> findCurrentPriceByProductId(@Param("productId") Long productId);

    /**
     * 특정 날짜에 적용되었던 가격 조회 (과거 데이터 계산용)
     * 시간을 무시하고 날짜만 비교
     */
    @Query("SELECT p FROM ProductPriceHistory p WHERE p.product.id = :productId " +
            "AND FUNCTION('DATE', p.effectiveFrom) <= FUNCTION('DATE', :targetDate) " +
            "AND (p.effectiveTo IS NULL OR FUNCTION('DATE', p.effectiveTo) > FUNCTION('DATE', :targetDate))")
    Optional<ProductPriceHistory> findPriceByProductIdAndDate(@Param("productId") Long productId,
            @Param("targetDate") LocalDateTime targetDate);

    /**
     * 제품의 모든 가격 이력 조회 (최신순)
     */
    List<ProductPriceHistory> findByProductIdOrderByEffectiveFromDesc(Long productId);

}