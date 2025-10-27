// DatabaseConfig.java - 데이터베이스 설정
package com.mynet.sales_management_system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 데이터베이스 관련 설정
 * - JPA Repository 활성화
 * - 트랜잭션 관리 활성화
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.mynet.sales_management_system.repository")
@EnableTransactionManagement
public class DatabaseConfig {
    // 추가 데이터베이스 설정이 필요한 경우 여기에 Bean 정의
}