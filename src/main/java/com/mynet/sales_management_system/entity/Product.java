package com.mynet.sales_management_system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code", nullable = false, unique = true)
    private String productCode; // 0001, 0002... 자동 생성

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(nullable = false)
    private String category; // 분류 (GX시리즈, 드럼 등)

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true; // 활성화 상태

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<ProductPriceHistory> priceHistory;

    // 현재 공급가 조회 메서드 추가
    @Transient
    public BigDecimal getCurrentSupplyPrice() {
        return priceHistory.stream()
                .filter(ph -> ph.getEffectiveTo() == null)
                .findFirst()
                .map(ProductPriceHistory::getSupplyPrice)
                .orElse(BigDecimal.ZERO);
    }
}