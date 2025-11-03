package com.mynet.sales_management_system.controller;

import com.mynet.sales_management_system.dto.ProductDTO;
import com.mynet.sales_management_system.entity.Product;
import com.mynet.sales_management_system.entity.ProductPriceHistory;
import com.mynet.sales_management_system.security.CustomUserDetails;
import com.mynet.sales_management_system.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;

    /**
     * 제품 분류 관리 페이지
     */
    @GetMapping
    public String productManagementPage(Model model) {
        log.info("제품 관리 페이지 접근");

        // 실제 모든 제품 조회 (비활성 포함)
        List<Product> allProducts = productService.getAllProducts();
        log.info("조회된 제품 수: {}", allProducts.size());

        // 카테고리별 그룹화를 위한 ProductDTO 변환
        Map<String, List<ProductDTO>> productsByCategory = allProducts.stream()
                .map(this::convertToDTO)
                .collect(Collectors.groupingBy(
                        ProductDTO::getCategory,
                        LinkedHashMap::new,
                        Collectors.toList()));

        // 기존 카테고리 목록
        List<String> existingCategories = productService.getCategories();

        // 다음 제품 코드
        String nextProductCode = getNextProductCodeFromDB();

        model.addAttribute("productsByCategory", productsByCategory);
        model.addAttribute("existingCategories", existingCategories);
        model.addAttribute("nextProductCode", nextProductCode);

        return "admin/product-management"; // 독립적인 템플릿 반환
    }

    /**
     * 제품 등록
     */
    @PostMapping("/register")
    @ResponseBody
    public ResponseEntity<String> registerProduct(
            @RequestBody Map<String, Object> request) {

        try {
            log.info("제품 등록 요청: {}", request);

            String category = (String) request.get("category");
            String productName = (String) request.get("productName");
            BigDecimal costPrice = new BigDecimal(request.get("costPrice").toString());
            BigDecimal supplyPrice = new BigDecimal(request.get("supplyPrice").toString());

            productService.createProduct(productName, category, costPrice, supplyPrice, "admin");

            return ResponseEntity.ok("제품이 성공적으로 등록되었습니다.");

        } catch (Exception e) {
            log.error("제품 등록 실패", e);
            return ResponseEntity.badRequest().body("제품 등록 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 제품 활성화
     */
    @PostMapping("/{productId}/activate")
    @ResponseBody
    public ResponseEntity<String> activateProduct(@PathVariable Long productId) {
        try {
            productService.updateProductActiveStatus(productId, true);
            return ResponseEntity.ok("제품이 활성화되었습니다.");
        } catch (Exception e) {
            log.error("제품 활성화 실패", e);
            return ResponseEntity.badRequest().body("제품 활성화 중 오류가 발생했습니다.");
        }
    }

    /**
     * 제품 비활성화
     */
    @PostMapping("/{productId}/deactivate")
    @ResponseBody
    public ResponseEntity<String> deactivateProduct(@PathVariable Long productId) {
        try {
            productService.updateProductActiveStatus(productId, false);
            return ResponseEntity.ok("제품이 비활성화되었습니다.");
        } catch (Exception e) {
            log.error("제품 비활성화 실패", e);
            return ResponseEntity.badRequest().body("제품 비활성화 중 오류가 발생했습니다.");
        }
    }

    /**
     * 제품 가격 수정
     */
    @PostMapping("/{productId}/update-price")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateProductPrice(
            @PathVariable Long productId,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Map<String, Object> response = new HashMap<>();

        try {
            // null 체크 추가
            BigDecimal costPrice = null;
            BigDecimal supplyPrice = null;

            if (request.get("costPrice") != null) {
                costPrice = new BigDecimal(request.get("costPrice").toString());
            }

            if (request.get("supplyPrice") != null) {
                supplyPrice = new BigDecimal(request.get("supplyPrice").toString());
            }

            // 둘 다 null이면 에러
            if (costPrice == null && supplyPrice == null) {
                response.put("success", false);
                response.put("message", "수정할 가격 정보가 없습니다.");
                return ResponseEntity.badRequest().body(response);
            }

            // 현재 로그인한 사용자로 수정
            String updatedBy = userDetails != null ? userDetails.getUsername() : "admin";

            productService.updateProductPrice(productId, costPrice, supplyPrice, updatedBy);

            response.put("success", true);
            response.put("message", "가격이 성공적으로 수정되었습니다.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("가격 수정 실패", e);
            response.put("success", false);
            response.put("message", "가격 수정 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Product 엔티티를 ProductDTO로 변환
     */
    private ProductDTO convertToDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setProductCode(product.getProductCode());
        dto.setProductName(product.getProductName());
        dto.setCategory(product.getCategory());
        dto.setActive(product.getIsActive());
        dto.setCreatedAt(product.getCreatedAt());
        dto.setCategoryClass(getCategoryClass(product.getCategory()));

        // 현재 가격 정보 설정
        Optional<ProductPriceHistory> currentPrice = productService.getCurrentProductPrice(product.getId());
        if (currentPrice.isPresent()) {
            ProductPriceHistory price = currentPrice.get();
            dto.setCurrentCostPrice(price.getCostPrice());
            dto.setCurrentSupplyPrice(price.getSupplyPrice());
            dto.setLastModifiedAt(price.getCreatedAt());
            dto.setLastModifiedBy(price.getCreatedBy());
        } else {
            dto.setCurrentCostPrice(BigDecimal.ZERO);
            dto.setCurrentSupplyPrice(BigDecimal.ZERO);
            dto.setLastModifiedAt(null);
            dto.setLastModifiedBy(null);
        }

        return dto;
    }

    /**
     * 카테고리별 CSS 클래스 할당
     */
    private String getCategoryClass(String category) {
        List<String> categories = productService.getCategories();
        int index = categories.indexOf(category);
        String[] colorClasses = { "green-1", "green-2", "green-3", "green-4" };
        return colorClasses[index % colorClasses.length];
    }

    /**
     * DB에서 다음 제품 코드 조회
     */
    private String getNextProductCodeFromDB() {
        List<Product> allProducts = productService.getAllProducts();
        if (allProducts.isEmpty()) {
            return "0001";
        }

        // 가장 큰 제품 코드 찾기
        int maxCode = allProducts.stream()
                .mapToInt(p -> Integer.parseInt(p.getProductCode()))
                .max()
                .orElse(0);

        return String.format("%04d", maxCode + 1);
    }
}