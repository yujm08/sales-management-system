// ProductPriceHistory.java - 제품 가격 이력 관리
package com.mynet.sales_management_system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_price_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductPriceHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @Column(name = "cost_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal costPrice; // 원가
    
    @Column(name = "supply_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal supplyPrice; // 공급가 (판매가)
    
    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom; // 적용 시작일
    
    @Column(name = "effective_to")
    private LocalDateTime effectiveTo; // 적용 종료일 (NULL이면 현재 적용 중)
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "created_by", nullable = false)
    private String createdBy; // 수정한 사용자
}