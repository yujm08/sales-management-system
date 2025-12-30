// ProductRepository.java - 제품 데이터 접근
package com.mynet.sales_management_system.repository;

import com.mynet.sales_management_system.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 제품 데이터 접근 레포지토리
 * - 제품 조회 및 관리
 * - 제품 코드 자동 생성 지원
 * - 활성/비활성 제품 구분 조회
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * 제품 코드로 조회
     */
    Optional<Product> findByProductCode(String productCode);

    /**
     * 제품명으로 조회 (검색 자동완성용)
     */
    List<Product> findByProductNameContainingIgnoreCase(String productName);

    /**
     * 활성화된 제품만 조회 (카테고리별, 공급가별, 제품코드별 정렬)
     */
    @Query("SELECT p FROM Product p WHERE p.isActive = true " +
            "ORDER BY p.category ASC, " +
            "(SELECT pph.supplyPrice FROM ProductPriceHistory pph " +
            " WHERE pph.product.id = p.id AND pph.effectiveTo IS NULL) ASC, " +
            "p.productCode ASC")
    List<Product> findByIsActiveTrueOrderByCategoryAscSupplyPriceAscProductCodeAsc();

    /**
     * 모든 제품 조회 (제품 분류 탭용 - 비활성 제품 포함)
     */
    List<Product> findAllByOrderByCategoryAscProductCodeAsc();

    /**
     * 카테고리별 조회
     */
    List<Product> findByCategoryOrderByProductCodeAsc(String category);

    /**
     * 다음 제품 코드 생성을 위한 최대 코드 조회
     */
    @Query("SELECT MAX(p.productCode) FROM Product p")
    Optional<String> findMaxProductCode();

    /**
     * 카테고리 목록 조회 (분류 선택용)
     */
    @Query("SELECT DISTINCT p.category FROM Product p ORDER BY p.category")
    List<String> findDistinctCategories();

    /*
     * 활성화된 제품만 조회 (카테고리별, 제품코드별 정렬)
     */
    @Query("SELECT p FROM Product p WHERE p.isActive = true ORDER BY p.category ASC, p.productCode ASC")
    List<Product> findByIsActiveTrueOrderByCategoryAscProductCodeAsc();

    /**
     * 활성화된 제품만 조회 (카테고리 내림차순, 제품명 오름차순)
     */
    @Query("SELECT p FROM Product p WHERE p.isActive = true ORDER BY p.category DESC, p.productName ASC")
    List<Product> findByIsActiveTrueOrderByCategoryDescProductNameAsc();
}