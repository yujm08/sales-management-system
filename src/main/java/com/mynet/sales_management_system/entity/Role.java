package com.mynet.sales_management_system.entity;

/**
 * 사용자 역할 열거형
 * - ADMIN: 관리자 권한 (모든 기능 접근 가능)
 * - USER: 일반 사용자 권한 (제한된 기능만 접근 가능)
 */
public enum Role {
    ADMIN,  // 관리자
    USER    // 일반 사용자
}