package com.mynet.sales_management_system.service;

import com.mynet.sales_management_system.dto.DailySalesStatusDTO;
import com.mynet.sales_management_system.dto.DailySalesStatusDTO.CompanySalesData;
import com.mynet.sales_management_system.dto.DailySalesStatusDTO.SalesData;
import com.mynet.sales_management_system.entity.Company;
import com.mynet.sales_management_system.entity.DailySales;
import com.mynet.sales_management_system.entity.Product;
import com.mynet.sales_management_system.entity.Target;
import com.mynet.sales_management_system.repository.CompanyRepository;
import com.mynet.sales_management_system.repository.DailySalesRepository;
import com.mynet.sales_management_system.repository.ProductRepository;
import com.mynet.sales_management_system.repository.TargetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 일일매출현황 서비스
 * - 제품별 회사별 일일/월간 판매 현황 집계
 * - 목표 수량 및 달성률 계산
 * - 전월/전년도 비교 데이터 제공
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DailySalesService {

        private final DailySalesRepository dailySalesRepository;
        private final CompanyRepository companyRepository;
        private final ProductRepository productRepository;
        private final TargetRepository targetRepository;
        private final ProductService productService;

        // 회사 순서 정의
        private static final List<String> COMPANY_ORDER = Arrays.asList(
                        "영현아이앤씨", "마이씨앤에스", "우리STM", "엠에스앤샵",
                        "원이스토리", "대현씨앤씨", "마이넷");

        /**
         * 일일매출현황 데이터 조회
         */
        public List<DailySalesStatusDTO> getDailySalesStatus(LocalDate targetDate) {
                log.info("일일매출현황 데이터 조회 시작: targetDate={}", targetDate);

                // 1. 회사 목록 조회 (캐논 제외, 순서대로)
                List<Company> companies = getOrderedCompanies();
                log.info("조회된 회사 수: {}", companies.size());

                // 2. 활성화된 제품 목록 조회
                List<Product> products = productRepository.findByIsActiveTrueOrderByCategoryDescProductNameAsc();
                log.info("활성화된 제품 수: {}", products.size());

                // 3. 각 제품별로 DTO 생성
                List<DailySalesStatusDTO> result = products.stream()
                                .map(product -> buildDailySalesStatusDTO(product, companies, targetDate))
                                .collect(Collectors.toList());

                log.info("일일매출현황 데이터 생성 완료: {} 건", result.size());
                return result;
        }

        /**
         * 회사 목록을 정해진 순서대로 조회
         */
        private List<Company> getOrderedCompanies() {
                List<Company> allCompanies = companyRepository.findByIsMynetFalse()
                                .stream()
                                .filter(c -> !"캐논".equals(c.getName()))
                                .collect(Collectors.toList());

                // 정해진 순서대로 정렬
                allCompanies.sort(Comparator.comparingInt(c -> {
                        String name = c.getName();
                        // "원이스토리(쿠팡)"은 "원이스토리"로 매칭
                        if (name.contains("원이스토리")) {
                                return COMPANY_ORDER.indexOf("원이스토리");
                        }
                        // "마이넷(GX판매)"는 "마이넷"으로 매칭
                        if (name.contains("마이넷")) {
                                return COMPANY_ORDER.indexOf("마이넷");
                        }
                        int index = COMPANY_ORDER.indexOf(name);
                        return index == -1 ? 999 : index;
                }));

                return allCompanies;
        }

        /**
         * 제품별 일일매출현황 DTO 생성
         */
        private DailySalesStatusDTO buildDailySalesStatusDTO(
                        Product product, List<Company> companies, LocalDate targetDate) {

                int year = targetDate.getYear();
                int month = targetDate.getMonthValue();
                int day = targetDate.getDayOfMonth();

                // 회사별 데이터 맵
                Map<String, CompanySalesData> companySalesMap = new LinkedHashMap<>();

                // 일 합계 변수
                int dailyTotalQuantity = 0;
                BigDecimal dailyTotalAmount = BigDecimal.ZERO;

                // 월 합계 변수
                int monthlyTotalQuantity = 0;
                BigDecimal monthlyTotalAmount = BigDecimal.ZERO;

                // 해당 날짜의 공급가 조회 (일 합계 금액 계산용)
                BigDecimal todaySupplyPrice = productService.getProductPriceAtDate(
                                product.getId(),
                                targetDate.atStartOfDay()).map(priceHistory -> priceHistory.getSupplyPrice())
                                .orElse(BigDecimal.ZERO);

                // 각 회사별 데이터 처리
                for (Company company : companies) {
                        log.info("=== 회사 처리 시작: {} (ID: {}) ===", company.getName(), company.getId());

                        // 일 수량 조회
                        Optional<DailySales> dailySalesOpt = dailySalesRepository
                                        .findByCompanyIdAndProductIdAndSalesDateNative(company.getId(), product.getId(),
                                                        targetDate);

                        Integer dailyQuantity = dailySalesRepository
                                        .findByCompanyIdAndProductIdAndSalesDate(company.getId(), product.getId(),
                                                        targetDate)
                                        .map(DailySales::getQuantity)
                                        .orElse(0);

                        log.info(">>> DB조회 테스트: 회사={}, 제품={}, 날짜={}, 결과={}",
                                        company.getName(), product.getProductCode(), targetDate,
                                        dailySalesOpt.isPresent() ? dailySalesOpt.get().getQuantity() : "없음");

                        // 월 기간의 일자별 판매 데이터 조회
                        LocalDate startDate = LocalDate.of(year, month, 1);
                        LocalDate endDate = targetDate;

                        List<DailySales> monthlySalesList = dailySalesRepository
                                        .findByCompanyIdAndProductIdBetweenDates(
                                                        company.getId(), product.getId(), startDate, endDate);

                        // 월 수량 합계
                        Integer monthlyQuantity = monthlySalesList.stream()
                                        .mapToInt(DailySales::getQuantity)
                                        .sum();

                        log.info("월 수량 조회: 기간={} ~ {}, 합계={}",
                                        startDate, endDate, monthlyQuantity);

                        // 월 금액 = 각 일자별 (수량 * 해당일 가격)의 합계
                        BigDecimal amount = BigDecimal.ZERO;
                        for (DailySales dailySale : monthlySalesList) {
                                BigDecimal priceOnThatDay = productService.getProductPriceAtDate(
                                                product.getId(),
                                                dailySale.getSalesDate().atStartOfDay())
                                                .map(priceHistory -> priceHistory.getSupplyPrice())
                                                .orElse(BigDecimal.ZERO);

                                BigDecimal dailyAmount = priceOnThatDay
                                                .multiply(BigDecimal.valueOf(dailySale.getQuantity()));
                                amount = amount.add(dailyAmount);

                                log.debug("  일자={}, 수량={}, 가격={}, 금액={}",
                                                dailySale.getSalesDate(), dailySale.getQuantity(), priceOnThatDay,
                                                dailyAmount);
                        }

                        log.info("최종 월 금액 계산: {}", amount);

                        // DB 원본 이름 사용 (정제하지 않음!)
                        String dbCompanyName = company.getName();
                        String mappedName;

                        // HTML 키와 정확히 일치하도록 매핑
                        if (dbCompanyName.equals("영현아이앤씨")) {
                                mappedName = "영현아이앤씨";
                        } else if (dbCompanyName.equals("마이씨앤에스")) {
                                mappedName = "마이씨앤에스";
                        } else if (dbCompanyName.equals("우리STM")) {
                                mappedName = "우리STM";
                        } else if (dbCompanyName.equals("엠에스앤샵")) {
                                mappedName = "엠에스앤샵";
                        } else if (dbCompanyName.equals("원이스토리 (쿠팡)") || dbCompanyName.contains("원이스토리")) {
                                mappedName = "원이스토리(쿠팡)"; // HTML 키는 띄어쓰기 없음
                        } else if (dbCompanyName.equals("대현씨앤씨")) {
                                mappedName = "대현씨앤씨";
                        } else if (dbCompanyName.equals("마이넷(GX판매)")
                                        || (dbCompanyName.contains("마이넷") && !dbCompanyName.equals("마이넷"))) {
                                mappedName = "마이넷(GX판매)";
                        } else {
                                mappedName = dbCompanyName;
                                log.warn("매핑되지 않은 회사명: '{}'", dbCompanyName);
                        }

                        log.info("회사명 매칭: DB='{}' → HTML='{}', 일수량={}, 월수량={}",
                                        dbCompanyName, mappedName, dailyQuantity, monthlyQuantity);

                        companySalesMap.put(mappedName, CompanySalesData.builder()
                                        .dailyQuantity(dailyQuantity)
                                        .monthlyQuantity(monthlyQuantity)
                                        .amount(amount)
                                        .build());

                        // 합계 누적
                        dailyTotalQuantity += dailyQuantity;
                        monthlyTotalQuantity += monthlyQuantity;
                        dailyTotalAmount = dailyTotalAmount
                                        .add(todaySupplyPrice.multiply(BigDecimal.valueOf(dailyQuantity)));
                        monthlyTotalAmount = monthlyTotalAmount.add(amount);
                }

                // DTO 생성 후 companySalesMap 확인
                log.info("=== companySalesMap 내용 (제품: {}) ===", product.getProductCode());
                companySalesMap.forEach(
                                (key, value) -> log.info("  키='{}': 일={}, 월={}, 금액={}", key, value.getDailyQuantity(),
                                                value.getMonthlyQuantity(), value.getAmount()));

                // 목표 수량 조회 (모든 회사의 목표 수량 합계)
                Integer targetQuantity = targetRepository
                                .findAllCompanyTargetsByProductAndYearAndMonth(product.getId(), year, month)
                                .stream()
                                .mapToInt(Target::getTargetQuantity)
                                .sum();

                log.debug("목표 수량 조회: 제품={}, 년월={}/{}, 목표={}",
                                product.getProductCode(), year, month, targetQuantity);

                // 비교 데이터 조회
                Integer prevMonthSales = getPrevMonthSales(product.getId(), year, month);
                Integer yearToDateSales = getYearToDateSales(product.getId(), year, month, targetDate);
                Integer lastYearPrevMonthSales = getLastYearPrevMonthSales(product.getId(), year, month);
                Integer lastYearCurrentMonthSales = getLastYearCurrentMonthSales(product.getId(), year, month);
                Integer lastYearYearToDateSales = getLastYearYearToDateSales(product.getId(), year, month);

                return DailySalesStatusDTO.builder()
                                .category(product.getCategory())
                                .productCode(product.getProductCode())
                                .productName(product.getProductName())
                                .supplyPrice(todaySupplyPrice)
                                .companySalesMap(companySalesMap)
                                .dailyTotal(SalesData.builder()
                                                .quantity(dailyTotalQuantity)
                                                .amount(dailyTotalAmount)
                                                .build())
                                .monthlyTotal(SalesData.builder()
                                                .quantity(monthlyTotalQuantity)
                                                .amount(monthlyTotalAmount)
                                                .build())
                                .targetQuantity(targetQuantity)
                                .prevMonthSales(prevMonthSales)
                                .yearToDateSales(yearToDateSales)
                                .lastYearPrevMonthSales(lastYearPrevMonthSales)
                                .lastYearCurrentMonthSales(lastYearCurrentMonthSales)
                                .lastYearYearToDateSales(lastYearYearToDateSales)
                                .build();
        }

        /**
         * 전월 판매량 조회
         */
        private Integer getPrevMonthSales(Long productId, int year, int month) {
                LocalDate prevMonth = LocalDate.of(year, month, 1).minusMonths(1);
                return dailySalesRepository
                                .findByProductIdAndYearAndMonthForComparison(
                                                productId, prevMonth.getYear(), prevMonth.getMonthValue())
                                .stream()
                                .mapToInt(DailySales::getQuantity)
                                .sum();
        }

        /**
         * M년 (N-1)~N월 판매량 (최근 2개월 누적) - targetDate까지만
         */
        private Integer getYearToDateSales(Long productId, int year, int month, LocalDate targetDate) {
                // 전월 전체
                LocalDate prevMonthStart = LocalDate.of(year, month, 1).minusMonths(1);
                LocalDate prevMonthEnd = prevMonthStart.withDayOfMonth(prevMonthStart.lengthOfMonth());

                int prevMonthQty = dailySalesRepository
                                .findByProductIdBetweenDates(productId, prevMonthStart, prevMonthEnd)
                                .stream()
                                .mapToInt(DailySales::getQuantity)
                                .sum();

                // 당월은 1일 ~ targetDate까지만
                LocalDate currentMonthStart = LocalDate.of(year, month, 1);

                int currentMonthQty = dailySalesRepository
                                .findByProductIdBetweenDates(productId, currentMonthStart, targetDate)
                                .stream()
                                .mapToInt(DailySales::getQuantity)
                                .sum();

                return prevMonthQty + currentMonthQty;
        }

        /**
         * (M-1)년 (N-1)월 판매량 (작년 이전 달)
         */
        private Integer getLastYearPrevMonthSales(Long productId, int year, int month) {
                LocalDate lastYearPrevMonth = LocalDate.of(year - 1, month, 1).minusMonths(1);
                return dailySalesRepository
                                .findByProductIdAndYearAndMonthForComparison(
                                                productId, lastYearPrevMonth.getYear(),
                                                lastYearPrevMonth.getMonthValue())
                                .stream()
                                .mapToInt(DailySales::getQuantity)
                                .sum();
        }

        /**
         * (M-1)년 N월 판매량 (작년 현재 달)
         */
        private Integer getLastYearCurrentMonthSales(Long productId, int year, int month) {
                return dailySalesRepository
                                .findByProductIdAndYearAndMonthForComparison(productId, year - 1, month)
                                .stream()
                                .mapToInt(DailySales::getQuantity)
                                .sum();
        }

        /**
         * (M-1)년 (N-1)~N월 판매량 (작년 최근 2개월 누적)
         */
        private Integer getLastYearYearToDateSales(Long productId, int year, int month) {
                // 작년 전월 계산 (연도 넘어가는 경우 처리)
                LocalDate lastYearPrevMonth = LocalDate.of(year - 1, month, 1).minusMonths(1);
                int prevMonthYear = lastYearPrevMonth.getYear();
                int prevMonthValue = lastYearPrevMonth.getMonthValue();

                // 작년 전월 판매량
                int prevMonthQty = dailySalesRepository
                                .findByProductIdAndYearAndMonthForComparison(productId, prevMonthYear, prevMonthValue)
                                .stream()
                                .mapToInt(DailySales::getQuantity)
                                .sum();

                // 작년 당월 판매량
                int currentMonthQty = dailySalesRepository
                                .findByProductIdAndYearAndMonthForComparison(productId, year - 1, month)
                                .stream()
                                .mapToInt(DailySales::getQuantity)
                                .sum();

                return prevMonthQty + currentMonthQty;
        }

        /**
         * DB 회사명을 HTML 표시용 키로 매핑
         */
        private String mapCompanyNameForDisplay(String dbCompanyName) {
                if (dbCompanyName == null)
                        return "";

                // 정확히 일치하는 경우
                switch (dbCompanyName) {
                        case "영현아이앤씨":
                                return "영현아이앤씨";
                        case "마이씨앤에스":
                                return "마이씨앤에스";
                        case "우리STM":
                                return "우리STM";
                        case "엠에스앤샵":
                                return "엠에스앤샵";
                        case "원이스토리(쿠팡)":
                                return "원이스토리 (쿠팡)";
                        case "대현씨앤씨":
                                return "대현씨앤씨";
                        case "마이넷(GX판매)":
                                return "마이넷(GX판매)";
                }

                // 부분 일치 (레거시 데이터 대응)
                if (dbCompanyName.contains("원이스토리"))
                        return "원이스토리(쿠팡)";
                if (dbCompanyName.contains("마이넷"))
                        return "마이넷(GX판매)";

                log.warn("매핑되지 않은 회사명: '{}'", dbCompanyName);
                return dbCompanyName; // 기본값
        }
}