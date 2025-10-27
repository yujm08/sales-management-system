// DateUtil.java - 날짜 관련 유틸리티
package com.mynet.sales_management_system.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * 날짜 관련 유틸리티 클래스
 * - Asia/Seoul 타임존 기준 처리
 * - 당월/당년 여부 확인
 * - 날짜 포맷팅
 */
@Component
public class DateUtil {
    
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 현재 한국 시간 기준 날짜 반환
     */
    public static LocalDate getCurrentDate() {
        return LocalDate.now(KOREA_ZONE);
    }
    
    /**
     * 현재 한국 시간 기준 날짜시간 반환
     */
    public static LocalDateTime getCurrentDateTime() {
        return LocalDateTime.now(KOREA_ZONE);
    }
    
    /**
     * 당월 데이터인지 확인 (하위회사 수정 권한 체크용)
     */
    public static boolean isCurrentMonth(LocalDate date) {
        YearMonth currentMonth = YearMonth.now(KOREA_ZONE);
        YearMonth targetMonth = YearMonth.from(date);
        return currentMonth.equals(targetMonth);
    }
    
    /**
     * 당년 데이터인지 확인
     */
    public static boolean isCurrentYear(LocalDate date) {
        int currentYear = LocalDate.now(KOREA_ZONE).getYear();
        return date.getYear() == currentYear;
    }
    
    /**
     * 날짜를 문자열로 포맷팅
     */
    public static String formatDate(LocalDate date) {
        return date.format(DATE_FORMATTER);
    }
    
    /**
     * 날짜시간을 문자열로 포맷팅
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DATETIME_FORMATTER);
    }
    
    /**
     * 월의 첫 번째 날 반환
     */
    public static LocalDate getFirstDayOfMonth(int year, int month) {
        return LocalDate.of(year, month, 1);
    }
    
    /**
     * 월의 마지막 날 반환
     */
    public static LocalDate getLastDayOfMonth(int year, int month) {
        return YearMonth.of(year, month).atEndOfMonth();
    }
}