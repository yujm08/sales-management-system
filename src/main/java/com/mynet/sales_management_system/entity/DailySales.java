// DailySales.java - 일별 실적 데이터
package com.mynet.sales_management_system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_sales", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "product_id", "sales_date"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailySales {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @Column(name = "sales_date", nullable = false)
    private LocalDate salesDate;
    
    @Column(nullable = false)
    private Integer quantity = 0; // 판매 수량
    
    @UpdateTimestamp
    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;
    
    @Column(name = "modified_by")
    private String modifiedBy; // 마지막 수정자
}