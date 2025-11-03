// ProductService.java - 제품 관리 서비스
package com.mynet.sales_management_system.service;

import com.mynet.sales_management_system.entity.Product;
import com.mynet.sales_management_system.entity.ProductPriceHistory;
import com.mynet.sales_management_system.repository.ProductRepository;
import com.mynet.sales_management_system.repository.ProductPriceHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mynet.sales_management_system.dto.ProductInputDTO;
import com.mynet.sales_management_system.entity.DailySales;
import com.mynet.sales_management_system.repository.DailySalesRepository;
import java.time.LocalDate;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 제품 관리 서비스
 * - 제품 등록/수정/조회
 * - 제품 코드 자동 생성 (0001, 0002...)
 * - 가격 이력 관리
 * - 활성/비활성 상태 관리
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductPriceHistoryRepository priceHistoryRepository;
    private final DailySalesRepository dailySalesRepository;

    /**
     * 활성화된 제품 목록 조회 (일상 입력용)
     */
    public List<Product> getActiveProducts() {
        return productRepository.findByIsActiveTrueOrderByCategoryAscSupplyPriceAscProductCodeAsc();
    }

    /**
     * 모든 제품 조회 (제품 분류 탭용 - 비활성 포함)
     */
    public List<Product> getAllProducts() {
        return productRepository.findAllByOrderByCategoryAscProductCodeAsc();
    }

    /**
     * 제품 코드로 조회
     */
    public Optional<Product> getProductByCode(String productCode) {
        return productRepository.findByProductCode(productCode);
    }

    /**
     * 제품명 검색 (자동완성용)
     */
    public List<Product> searchProductsByName(String productName) {
        return productRepository.findByProductNameContainingIgnoreCase(productName);
    }

    /**
     * 카테고리 목록 조회
     */
    public List<String> getCategories() {
        return productRepository.findDistinctCategories();
    }

    /**
     * 새 제품 등록
     * - 제품 코드 자동 생성
     * - 초기 가격 이력 생성
     */
    @Transactional
    public Product createProduct(String productName, String category,
            BigDecimal costPrice, BigDecimal supplyPrice, String createdBy) {

        // 새 제품 코드 생성
        String newProductCode = generateNextProductCode();

        // 제품 생성
        Product product = Product.builder()
                .productCode(newProductCode)
                .productName(productName)
                .category(category)
                .isActive(true)
                .build();

        product = productRepository.save(product);

        // 초기 가격 이력 생성
        ProductPriceHistory priceHistory = ProductPriceHistory.builder()
                .product(product)
                .costPrice(costPrice)
                .supplyPrice(supplyPrice)
                .effectiveFrom(LocalDateTime.now())
                .effectiveTo(null) // 현재 적용 중
                .createdBy(createdBy)
                .build();

        priceHistoryRepository.save(priceHistory);

        log.info("새 제품 등록: 코드={}, 이름={}, 분류={}", newProductCode, productName, category);
        return product;
    }

    /**
     * 제품 가격 업데이트
     * - 기존 가격 이력의 effective_to를 현재 시간으로 설정
     * - 새 가격 이력 생성
     */
    @Transactional
    public void updateProductPrice(Long productId, BigDecimal costPrice,
            BigDecimal supplyPrice, String updatedBy) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("제품을 찾을 수 없습니다."));

        // 현재 가격 조회
        ProductPriceHistory currentPrice = priceHistoryRepository
                .findCurrentPriceByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("현재 가격 정보를 찾을 수 없습니다."));

        // null이면 기존 값 유지
        BigDecimal newCostPrice = costPrice != null ? costPrice : currentPrice.getCostPrice();
        BigDecimal newSupplyPrice = supplyPrice != null ? supplyPrice : currentPrice.getSupplyPrice();

        // 기존 가격 이력 종료
        currentPrice.setEffectiveTo(LocalDateTime.now());
        priceHistoryRepository.save(currentPrice);

        // 새 가격 이력 생성
        ProductPriceHistory newPriceHistory = ProductPriceHistory.builder()
                .product(product)
                .costPrice(newCostPrice)
                .supplyPrice(newSupplyPrice)
                .effectiveFrom(LocalDateTime.now())
                .effectiveTo(null)
                .createdBy(updatedBy)
                .build();

        priceHistoryRepository.save(newPriceHistory);

        log.info("제품 가격 수정: 제품ID={}, 원가={}, 공급가={}, 수정자={}",
                productId, newCostPrice, newSupplyPrice, updatedBy);
    }

    /**
     * 제품 활성/비활성 상태 변경
     */
    @Transactional
    public void updateProductActiveStatus(Long productId, boolean isActive) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("제품을 찾을 수 없습니다: " + productId));

        product.setIsActive(isActive);
        productRepository.save(product);

        log.info("제품 상태 변경: 코드={}, 활성화={}", product.getProductCode(), isActive);
    }

    /**
     * 특정 날짜의 제품 가격 조회 (과거 데이터 계산용)
     */
    public Optional<ProductPriceHistory> getProductPriceAtDate(Long productId, LocalDateTime targetDate) {
        return priceHistoryRepository.findPriceByProductIdAndDate(productId, targetDate);
    }

    /**
     * 현재 제품 가격 조회
     */
    public Optional<ProductPriceHistory> getCurrentProductPrice(Long productId) {
        return priceHistoryRepository.findCurrentPriceByProductId(productId);
    }

    /**
     * 다음 제품 코드 생성 (0001, 0002, ...)
     * 중간에 삭제된 제품이 있어도 연번 건너뛰기
     */
    private String generateNextProductCode() {
        Optional<String> maxCodeOpt = productRepository.findMaxProductCode();

        if (maxCodeOpt.isEmpty()) {
            return "0001"; // 첫 번째 제품
        }

        String maxCode = maxCodeOpt.get();
        int nextNumber = Integer.parseInt(maxCode) + 1;

        return String.format("%04d", nextNumber); // 4자리 제로패딩
    }

    /**
     * 다음 제품 코드 조회 (컨트롤러용)
     */
    public String getNextProductCode() {
        return generateNextProductCode();
    }

    /**
     * 하위회사 입력용 제품 목록 조회 (카테고리별 그룹화)
     */
    public Map<String, List<ProductInputDTO>> getProductsForInput(Long companyId, LocalDate date) {
        // 1. 활성화된 모든 제품 조회 (is_active = true)
        List<Product> activeProducts = productRepository
                .findByIsActiveTrueOrderByCategoryAscSupplyPriceAscProductCodeAsc();

        // 2. 해당 날짜의 입력된 수량 조회
        List<DailySales> existingSales = dailySalesRepository
                .findByCompanyIdAndSalesDate(companyId, date);
        Map<Long, Integer> quantityMap = existingSales.stream()
                .collect(Collectors.toMap(
                        sales -> sales.getProduct().getId(),
                        DailySales::getQuantity));

        // 3. ProductInputDTO로 변환하고 카테고리별 그룹화
        return activeProducts.stream()
                .map(product -> {
                    ProductInputDTO dto = new ProductInputDTO();
                    dto.setId(product.getId());
                    dto.setCategory(product.getCategory());
                    dto.setProductCode(product.getProductCode());
                    dto.setProductName(product.getProductName());
                    dto.setSupplyPrice(product.getCurrentSupplyPrice());
                    dto.setQuantity(quantityMap.getOrDefault(product.getId(), 0));
                    dto.setCategoryClass(getCategoryClass(product.getCategory()));
                    return dto;
                })
                .collect(Collectors.groupingBy(
                        ProductInputDTO::getCategory,
                        LinkedHashMap::new, // 순서 유지
                        Collectors.toList()));
    }

    /**
     * 카테고리별 CSS 클래스 할당
     */
    private String getCategoryClass(String category) {
        List<String> categories = getCategories();
        int index = categories.indexOf(category);
        String[] colorClasses = { "light-green", "medium-green", "dark-green", "lighter-green" };
        return colorClasses[index % colorClasses.length];
    }
}