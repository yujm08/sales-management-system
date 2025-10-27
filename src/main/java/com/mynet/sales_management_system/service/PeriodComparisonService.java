package com.mynet.sales_management_system.service;

import com.mynet.sales_management_system.dto.PeriodComparisonDTO;
import com.mynet.sales_management_system.entity.DailySales;
import com.mynet.sales_management_system.entity.ProductPriceHistory;
import com.mynet.sales_management_system.repository.DailySalesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PeriodComparisonService {

    private final DailySalesRepository dailySalesRepository;
    private final ProductService productService;

    /**
     * 특정 제품의 기간별 비교 데이터 조회
     */
    public PeriodComparisonDTO.PeriodData getPeriodDataForProduct(
            Long productId, LocalDate startDate, LocalDate endDate) {
        
        // 전체 하위회사의 해당 제품 데이터 조회
        List<DailySales> salesList = dailySalesRepository
                .findByProductIdAndDateRange(productId, startDate, endDate);

        return calculatePeriodData(salesList, startDate, endDate);
    }

    /**
     * 전체 제품의 기간별 비교 데이터 조회 (제품 미선택 시)
     */
    public PeriodComparisonDTO.PeriodData getPeriodDataForAllProducts(
            LocalDate startDate, LocalDate endDate) {
        
        // 전체 하위회사, 전체 제품 데이터 조회
        List<DailySales> salesList = dailySalesRepository
                .findByDateRangeOrderByDateAndProduct(startDate, endDate);

        return calculatePeriodData(salesList, startDate, endDate);
    }

    /**
     * 기간 데이터 계산 (일별 상세 포함)
     */
    private PeriodComparisonDTO.PeriodData calculatePeriodData(
            List<DailySales> salesList, LocalDate startDate, LocalDate endDate) {

        // 날짜별로 그룹화
        Map<LocalDate, List<DailySales>> salesByDate = salesList.stream()
                .collect(Collectors.groupingBy(DailySales::getSalesDate));

        List<PeriodComparisonDTO.DailyData> dailyDetails = new ArrayList<>();
        Integer totalQuantity = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        // 날짜 범위를 순회하며 일별 데이터 생성
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            List<DailySales> daySales = salesByDate.getOrDefault(currentDate, new ArrayList<>());
            
            int dayQuantity = 0;
            BigDecimal dayAmount = BigDecimal.ZERO;

            for (DailySales sales : daySales) {
                dayQuantity += sales.getQuantity();

                // 해당 날짜의 가격으로 금액 계산
                ProductPriceHistory priceHistory = productService
                        .getProductPriceAtDate(sales.getProduct().getId(), 
                                             currentDate.atStartOfDay())
                        .orElse(null);

                if (priceHistory != null) {
                    BigDecimal amount = priceHistory.getSupplyPrice()
                            .multiply(BigDecimal.valueOf(sales.getQuantity()));
                    dayAmount = dayAmount.add(amount);
                }
            }

            dailyDetails.add(PeriodComparisonDTO.DailyData.builder()
                    .date(currentDate)
                    .quantity(dayQuantity)
                    .amount(dayAmount)
                    .build());

            totalQuantity += dayQuantity;
            totalAmount = totalAmount.add(dayAmount);

            currentDate = currentDate.plusDays(1);
        }

        return PeriodComparisonDTO.PeriodData.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalQuantity(totalQuantity)
                .totalAmount(totalAmount)
                .dailyDetails(dailyDetails)
                .expanded(false)
                .build();
    }
}