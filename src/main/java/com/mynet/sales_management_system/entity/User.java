//User.java - 사용자 계정 정보

package com.mynet.sales_management_system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String username;
    
    @Column(nullable = false)
    private String password;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role; // ADMIN, USER
    
    @Column(name = "is_canon", nullable = false)
    private Boolean isCanon = false; // 캐논 계정 여부
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}