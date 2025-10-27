// Target.java - 목표 설정
package com.mynet.sales_management_system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "targets",
       uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "product_id", "target_year", "target_month"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Target {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id") // NULL 가능 (전체 목표인 경우)
    private Company company;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @Column(name = "target_year", nullable = false)
    private Integer targetYear;
    
    @Column(name = "target_month", nullable = false)
    private Integer targetMonth;
    
    @Column(name = "target_quantity", nullable = false)
    private Integer targetQuantity; // 목표 수량
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "created_by", nullable = false)
    private String createdBy; // 목표 설정자
}