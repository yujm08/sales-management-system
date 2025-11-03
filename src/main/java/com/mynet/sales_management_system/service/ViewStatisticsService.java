package com.mynet.sales_management_system.service;
// ViewStatisticsService.java - 마이넷 조회용 통계 서비스

import com.mynet.sales_management_system.dto.ViewStatisticsDTO;
import com.mynet.sales_management_system.entity.*;
import com.mynet.sales_management_system.repository.*;
import com.mynet.sales_management_system.util.PriceCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ViewStatisticsService {

        private final DailySalesRepository dailySalesRepository;
        private final ProductRepository productRepository;
        private final TargetRepository targetRepository;
        private final ProductService productService;

        /**
         * 마이넷 조회 페이지용 통계 데이터 생성
         */
        public List<ViewStatisticsDTO> getViewStatistics(String companyFilter, LocalDate targetDate) {
                List<Product> activeProducts = productRepository.findByIsActiveTrueOrderByCategoryAscProductCodeAsc();

                if ("all".equals(companyFilter)) {
                        return generateAllCompanyStatistics(activeProducts, targetDate);
                } else {
                        Long companyId = Long.parseLong(companyFilter);
                        return generateSingleCompanyStatistics(activeProducts, companyId, targetDate);
                }
        }

        /**
         * 전체 회사 통계 생성
         */
        private List<ViewStatisticsDTO> generateAllCompanyStatistics(List<Product> products, LocalDate targetDate) {
                List<ViewStatisticsDTO> result = new ArrayList<>();
                Map<String, List<ViewStatisticsDTO>> categoryMap = new LinkedHashMap<>();

                for (Product product : products) {
                        // 모든 회사의 해당 제품 데이터 조회
                        List<DailySales> dailySalesList = dailySalesRepository.findByProductIdAndSalesDate(
                                        product.getId(),
                                        targetDate);
                        List<DailySales> monthlySalesList = dailySalesRepository.findByProductIdAndYearAndMonth(
                                        product.getId(), targetDate.getYear(), targetDate.getMonthValue(),
                                        targetDate.getDayOfMonth());

                        // 일별/월별 합계 계산
                        ViewStatisticsDTO.DailySummary dailySummary = calculateDailySummary(dailySalesList, product,
                                        targetDate);
                        ViewStatisticsDTO.MonthlySummary monthlySummary = calculateMonthlySummary(monthlySalesList,
                                        product,
                                        targetDate);
                        ViewStatisticsDTO.TargetData targetData = calculateTargetData(
                                        product, targetDate, monthlySummary.getQuantity(), null // 전체(Global)
                        );

                        // 회사별 상세 데이터
                        List<ViewStatisticsDTO.CompanyData> companyDataList = generateCompanyDataList(dailySalesList,
                                        product, targetDate);

                        ViewStatisticsDTO dto = ViewStatisticsDTO.builder()
                                        .productId(product.getId())
                                        .category(product.getCategory())
                                        .productCode(product.getProductCode())
                                        .productName(product.getProductName())
                                        .costPrice(getCostPriceAtDate(product, targetDate))
                                        .supplyPrice(getSupplyPriceAtDate(product, targetDate))
                                        .dailySummary(dailySummary)
                                        .monthlySummary(monthlySummary)
                                        .targetData(targetData)
                                        .companyDataList(companyDataList)
                                        .build();

                        categoryMap.computeIfAbsent(product.getCategory(), k -> new ArrayList<>()).add(dto);
                }

                // 카테고리별로 정렬하여 결과 생성
                for (Map.Entry<String, List<ViewStatisticsDTO>> entry : categoryMap.entrySet()) {
                        result.addAll(entry.getValue());
                        // 소계 추가
                        result.add(generateSubtotal(entry.getKey(), entry.getValue()));
                }

                // 전체 합계 추가
                result.add(generateGrandTotal(result));

                return result;
        }

        /**
         * 단일 회사 통계 생성
         */
        private List<ViewStatisticsDTO> generateSingleCompanyStatistics(List<Product> products, Long companyId,
                        LocalDate targetDate) {
                List<ViewStatisticsDTO> result = new ArrayList<>();
                Map<String, List<ViewStatisticsDTO>> categoryMap = new LinkedHashMap<>();

                for (Product product : products) {
                        // 해당 회사의 제품 데이터만 조회
                        Optional<DailySales> dailySalesOpt = dailySalesRepository
                                        .findByCompanyIdAndProductIdAndSalesDate(
                                                        companyId, product.getId(), targetDate);
                        List<DailySales> monthlySalesList = dailySalesRepository
                                        .findByCompanyIdAndProductIdAndYearAndMonth(
                                                        companyId, product.getId(), targetDate.getYear(),
                                                        targetDate.getMonthValue(),
                                                        targetDate.getDayOfMonth());

                        DailySales dailySales = dailySalesOpt.orElse(null);
                        ViewStatisticsDTO.DailySummary dailySummary = calculateSingleDailySummary(dailySales, product,
                                        targetDate,
                                        companyId);
                        ViewStatisticsDTO.MonthlySummary monthlySummary = calculateSingleMonthlySummary(
                                        monthlySalesList, product,
                                        targetDate, companyId);
                        ViewStatisticsDTO.TargetData targetData = calculateTargetData(
                                        product, targetDate, monthlySummary.getQuantity(), companyId // 특정 회사
                        );

                        ViewStatisticsDTO dto = ViewStatisticsDTO.builder()
                                        .productId(product.getId())
                                        .category(product.getCategory())
                                        .productCode(product.getProductCode())
                                        .productName(product.getProductName())
                                        .costPrice(getCostPriceAtDate(product, targetDate))
                                        .supplyPrice(getSupplyPriceAtDate(product, targetDate))
                                        .dailySummary(dailySummary)
                                        .monthlySummary(monthlySummary)
                                        .targetData(targetData)
                                        .build();

                        categoryMap.computeIfAbsent(product.getCategory(), k -> new ArrayList<>()).add(dto);
                }

                // 카테고리별로 정렬하여 결과 생성
                for (Map.Entry<String, List<ViewStatisticsDTO>> entry : categoryMap.entrySet()) {
                        result.addAll(entry.getValue());
                        result.add(generateSubtotal(entry.getKey(), entry.getValue()));
                }

                result.add(generateGrandTotal(result));

                return result;
        }

        /**
         * 일별 합계 계산 (전체 회사)
         */
        private ViewStatisticsDTO.DailySummary calculateDailySummary(List<DailySales> salesList, Product product,
                        LocalDate targetDate) {
                int totalQuantity = salesList.stream().mapToInt(DailySales::getQuantity).sum();

                LocalDateTime queryTime = getQueryDateTime(targetDate);
                ProductPriceHistory priceHistory = productService.getProductPriceAtDate(
                                product.getId(), queryTime).orElse(null);

                PriceCalculator.SalesAmount salesAmount = PriceCalculator.calculateSalesAmount(totalQuantity,
                                priceHistory);

                // 전일 대비 비교 데이터
                ViewStatisticsDTO.ComparisonData comparison = calculateDailyComparison(product.getId(), targetDate);

                // 최근 수정 시간 조회
                LocalDateTime lastModified = salesList.stream()
                                .map(DailySales::getLastModifiedAt)
                                .filter(Objects::nonNull)
                                .max(LocalDateTime::compareTo)
                                .orElse(null);

                String modifiedBy = salesList.stream()
                                .filter(s -> s.getLastModifiedAt() != null
                                                && s.getLastModifiedAt().equals(lastModified))
                                .map(DailySales::getModifiedBy)
                                .findFirst()
                                .orElse(null);

                return ViewStatisticsDTO.DailySummary.builder()
                                .quantity(totalQuantity)
                                .amount(salesAmount.getRevenue())
                                .profit(salesAmount.getProfit())
                                .profitRate(PriceCalculator.calculateProfitRate(salesAmount.getProfit(),
                                                salesAmount.getRevenue()))
                                .comparison(comparison)
                                .lastModifiedAt(lastModified)
                                .modifiedBy(modifiedBy)
                                .build();
        }

        /**
         * 단일 회사 일별 합계 계산
         */
        private ViewStatisticsDTO.DailySummary calculateSingleDailySummary(DailySales dailySales, Product product,
                        LocalDate targetDate, Long companyId) {
                int quantity = dailySales != null ? dailySales.getQuantity() : 0;

                LocalDateTime queryTime = getQueryDateTime(targetDate);
                ProductPriceHistory priceHistory = productService.getProductPriceAtDate(
                                product.getId(), queryTime).orElse(null);

                PriceCalculator.SalesAmount salesAmount = PriceCalculator.calculateSalesAmount(quantity, priceHistory);

                // 전일 대비 비교 데이터
                ViewStatisticsDTO.ComparisonData comparison = calculateSingleCompanyDailyComparison(companyId,
                                product.getId(),
                                targetDate);

                return ViewStatisticsDTO.DailySummary.builder()
                                .quantity(quantity)
                                .amount(salesAmount.getRevenue())
                                .profit(salesAmount.getProfit())
                                .profitRate(PriceCalculator.calculateProfitRate(salesAmount.getProfit(),
                                                salesAmount.getRevenue()))
                                .comparison(comparison)
                                .lastModifiedAt(dailySales != null ? dailySales.getLastModifiedAt() : null)
                                .modifiedBy(dailySales != null ? dailySales.getModifiedBy() : null)
                                .build();
        }

        /**
         * 월별 합계 계산 (선택된 날짜까지)- 각 날짜별 가격으로 계산
         */
        private ViewStatisticsDTO.MonthlySummary calculateMonthlySummary(List<DailySales> monthlySalesList,
                        Product product,
                        LocalDate targetDate) {
                BigDecimal totalRevenue = BigDecimal.ZERO;
                BigDecimal totalProfit = BigDecimal.ZERO;
                int totalQuantity = 0;

                // 각 날짜별로 해당 날짜의 가격 사용
                for (DailySales sales : monthlySalesList) {
                        LocalDateTime salesDateTime = getQueryDateTime(sales.getSalesDate());
                        ProductPriceHistory priceHistory = productService.getProductPriceAtDate(
                                        product.getId(), salesDateTime).orElse(null);

                        if (priceHistory != null) {
                                PriceCalculator.SalesAmount salesAmount = PriceCalculator.calculateSalesAmount(
                                                sales.getQuantity(), priceHistory);
                                totalRevenue = totalRevenue.add(salesAmount.getRevenue());
                                totalProfit = totalProfit.add(salesAmount.getProfit());
                        }

                        totalQuantity += sales.getQuantity();
                }

                BigDecimal profitRate = totalRevenue.compareTo(BigDecimal.ZERO) != 0
                                ? totalProfit.divide(totalRevenue, 4, BigDecimal.ROUND_HALF_UP)
                                                .multiply(BigDecimal.valueOf(100))
                                : BigDecimal.ZERO;

                // 전월 대비 비교
                ViewStatisticsDTO.ComparisonData comparison = calculateMonthlyComparison(product.getId(), targetDate);

                return ViewStatisticsDTO.MonthlySummary.builder()
                                .quantity(totalQuantity)
                                .amount(totalRevenue)
                                .profit(totalProfit)
                                .profitRate(profitRate)
                                .comparison(comparison)
                                .build();
        }

        /**
         * 단일 회사 월별 합계 계산
         */
        private ViewStatisticsDTO.MonthlySummary calculateSingleMonthlySummary(List<DailySales> monthlySalesList,
                        Product product, LocalDate targetDate, Long companyId) {

                BigDecimal totalRevenue = BigDecimal.ZERO;
                BigDecimal totalProfit = BigDecimal.ZERO;
                int totalQuantity = 0;

                // 각 날짜별로 해당 날짜의 가격 사용
                for (DailySales sales : monthlySalesList) {
                        LocalDateTime salesDateTime = getQueryDateTime(sales.getSalesDate());
                        ProductPriceHistory priceHistory = productService.getProductPriceAtDate(
                                        product.getId(), salesDateTime).orElse(null);

                        if (priceHistory != null) {
                                PriceCalculator.SalesAmount salesAmount = PriceCalculator.calculateSalesAmount(
                                                sales.getQuantity(), priceHistory);
                                totalRevenue = totalRevenue.add(salesAmount.getRevenue());
                                totalProfit = totalProfit.add(salesAmount.getProfit());
                        }

                        totalQuantity += sales.getQuantity();
                }

                BigDecimal profitRate = totalRevenue.compareTo(BigDecimal.ZERO) != 0
                                ? totalProfit.divide(totalRevenue, 4, BigDecimal.ROUND_HALF_UP)
                                                .multiply(BigDecimal.valueOf(100))
                                : BigDecimal.ZERO;

                // 전월 대비 비교
                ViewStatisticsDTO.ComparisonData comparison = calculateSingleCompanyMonthlyComparison(companyId,
                                product.getId(), targetDate);

                return ViewStatisticsDTO.MonthlySummary.builder()
                                .quantity(totalQuantity)
                                .amount(totalRevenue)
                                .profit(totalProfit)
                                .profitRate(profitRate)
                                .comparison(comparison)
                                .build();
        }

        /**
         * 목표 달성률 계산
         * 회사별 / 전체(Global) 분기 처리
         */
        private ViewStatisticsDTO.TargetData calculateTargetData(Product product, LocalDate targetDate,
                        Integer monthlyQuantity, Long companyId) {

                Integer targetQuantity = 0;

                if (companyId != null) {
                        // 특정 회사 목표 조회
                        Optional<Target> targetOpt = targetRepository
                                        .findByCompanyIdAndProductIdAndTargetYearAndTargetMonth(
                                                        companyId, product.getId(), targetDate.getYear(),
                                                        targetDate.getMonthValue());

                        if (targetOpt.isPresent()) {
                                targetQuantity = targetOpt.get().getTargetQuantity();
                        }
                } else {
                        // 전체(Global) 목표: 모든 회사의 목표 합계
                        List<Target> companyTargets = targetRepository.findAllCompanyTargetsByProductAndYearAndMonth(
                                        product.getId(), targetDate.getYear(), targetDate.getMonthValue());

                        targetQuantity = companyTargets.stream()
                                        .mapToInt(Target::getTargetQuantity)
                                        .sum();
                }

                BigDecimal achievementRate = PriceCalculator.calculateAchievementRate(
                                monthlyQuantity != null ? monthlyQuantity : 0,
                                targetQuantity);

                return ViewStatisticsDTO.TargetData.builder()
                                .targetMonth(targetDate.getMonthValue())
                                .targetQuantity(targetQuantity)
                                .achievementRate(achievementRate)
                                .build();
        }

        /**
         * 회사별 데이터 생성
         */
        private List<ViewStatisticsDTO.CompanyData> generateCompanyDataList(List<DailySales> salesList,
                        Product product, LocalDate targetDate) {
                Map<Long, List<DailySales>> companyGrouped = salesList.stream()
                                .collect(Collectors.groupingBy(s -> s.getCompany().getId()));

                return companyGrouped.entrySet().stream()
                                .map(entry -> {
                                        Long companyId = entry.getKey();
                                        List<DailySales> companySales = entry.getValue();

                                        String companyName = companySales.get(0).getCompany().getName();

                                        // 각 날짜별로 가격을 조회해서 계산
                                        BigDecimal totalRevenue = BigDecimal.ZERO;
                                        BigDecimal totalProfit = BigDecimal.ZERO;
                                        int totalQuantity = 0;

                                        for (DailySales sales : companySales) {
                                                LocalDateTime salesDateTime = getQueryDateTime(sales.getSalesDate());
                                                ProductPriceHistory priceHistory = productService.getProductPriceAtDate(
                                                                product.getId(), salesDateTime).orElse(null);

                                                if (priceHistory != null) {
                                                        PriceCalculator.SalesAmount salesAmount = PriceCalculator
                                                                        .calculateSalesAmount(
                                                                                        sales.getQuantity(),
                                                                                        priceHistory);
                                                        totalRevenue = totalRevenue.add(salesAmount.getRevenue());
                                                        totalProfit = totalProfit.add(salesAmount.getProfit());
                                                }

                                                totalQuantity += sales.getQuantity();
                                        }

                                        LocalDateTime lastModified = companySales.stream()
                                                        .map(DailySales::getLastModifiedAt)
                                                        .filter(Objects::nonNull)
                                                        .max(LocalDateTime::compareTo)
                                                        .orElse(null);

                                        String modifiedBy = companySales.stream()
                                                        .filter(s -> s.getLastModifiedAt() != null
                                                                        && s.getLastModifiedAt().equals(lastModified))
                                                        .map(DailySales::getModifiedBy)
                                                        .findFirst()
                                                        .orElse(null);

                                        return ViewStatisticsDTO.CompanyData.builder()
                                                        .companyId(companyId)
                                                        .companyName(companyName)
                                                        .quantity(totalQuantity)
                                                        .amount(totalRevenue)
                                                        .profit(totalProfit)
                                                        .lastModifiedAt(lastModified)
                                                        .modifiedBy(modifiedBy)
                                                        .build();
                                })
                                .collect(Collectors.toList());
        }

        /**
         * 전일 대비 비교 계산
         */
        private ViewStatisticsDTO.ComparisonData calculateDailyComparison(Long productId, LocalDate currentDate) {
                LocalDate previousDate = currentDate.minusDays(1);

                List<DailySales> currentSales = dailySalesRepository.findByProductIdAndSalesDate(productId,
                                currentDate);
                List<DailySales> previousSales = dailySalesRepository.findByProductIdAndSalesDate(productId,
                                previousDate);

                LocalDateTime currentQueryTime = getQueryDateTime(currentDate);
                LocalDateTime previousQueryTime = getQueryDateTime(previousDate);

                BigDecimal currentProfit = calculateTotalProfit(currentSales, productId, currentQueryTime);
                BigDecimal previousProfit = calculateTotalProfit(previousSales, productId, previousQueryTime);

                return buildComparisonData(currentProfit, previousProfit);
        }

        /**
         * 단일 회사 전일 대비 비교
         */
        private ViewStatisticsDTO.ComparisonData calculateSingleCompanyDailyComparison(Long companyId, Long productId,
                        LocalDate currentDate) {
                LocalDate previousDate = currentDate.minusDays(1);

                Optional<DailySales> currentSales = dailySalesRepository.findByCompanyIdAndProductIdAndSalesDate(
                                companyId,
                                productId, currentDate);
                Optional<DailySales> previousSales = dailySalesRepository.findByCompanyIdAndProductIdAndSalesDate(
                                companyId,
                                productId, previousDate);

                LocalDateTime currentQueryTime = getQueryDateTime(currentDate);
                LocalDateTime previousQueryTime = getQueryDateTime(previousDate);

                BigDecimal currentProfit = calculateSingleProfit(currentSales.orElse(null), productId,
                                currentQueryTime);
                BigDecimal previousProfit = calculateSingleProfit(previousSales.orElse(null), productId,
                                previousQueryTime);

                return buildComparisonData(currentProfit, previousProfit);
        }

        /**
         * 전월 대비 비교 계산
         */
        private ViewStatisticsDTO.ComparisonData calculateMonthlyComparison(Long productId, LocalDate currentDate) {
                LocalDate previousMonth = currentDate.minusMonths(1);

                List<DailySales> currentSales = dailySalesRepository.findByProductIdAndYearAndMonth(
                                productId, currentDate.getYear(), currentDate.getMonthValue(),
                                currentDate.getDayOfMonth());
                List<DailySales> previousSales = dailySalesRepository.findByProductIdAndYearAndMonth(
                                productId, previousMonth.getYear(), previousMonth.getMonthValue(),
                                previousMonth.getDayOfMonth());

                LocalDateTime currentQueryTime = getQueryDateTime(currentDate);
                LocalDateTime previousQueryTime = getQueryDateTime(previousMonth);

                BigDecimal currentProfit = calculateTotalProfit(currentSales, productId, currentQueryTime);
                BigDecimal previousProfit = calculateTotalProfit(previousSales, productId, previousQueryTime);

                return buildComparisonData(currentProfit, previousProfit);
        }

        /**
         * 단일 회사 전월 대비 비교
         */
        private ViewStatisticsDTO.ComparisonData calculateSingleCompanyMonthlyComparison(Long companyId, Long productId,
                        LocalDate currentDate) {
                LocalDate previousMonth = currentDate.minusMonths(1);

                List<DailySales> currentSales = dailySalesRepository.findByCompanyIdAndProductIdAndYearAndMonth(
                                companyId, productId, currentDate.getYear(), currentDate.getMonthValue(),
                                currentDate.getDayOfMonth());
                List<DailySales> previousSales = dailySalesRepository.findByCompanyIdAndProductIdAndYearAndMonth(
                                companyId, productId, previousMonth.getYear(), previousMonth.getMonthValue(),
                                previousMonth.getDayOfMonth());

                LocalDateTime currentQueryTime = getQueryDateTime(currentDate);
                LocalDateTime previousQueryTime = getQueryDateTime(previousMonth);

                BigDecimal currentProfit = calculateTotalProfit(currentSales, productId, currentQueryTime);
                BigDecimal previousProfit = calculateTotalProfit(previousSales, productId, previousQueryTime);

                return buildComparisonData(currentProfit, previousProfit);
        }

        /**
         * 비교 데이터 빌드
         */
        private ViewStatisticsDTO.ComparisonData buildComparisonData(BigDecimal currentValue,
                        BigDecimal previousValue) {
                BigDecimal changeAmount = currentValue.subtract(previousValue);
                BigDecimal changeRate = BigDecimal.ZERO;

                if (previousValue.compareTo(BigDecimal.ZERO) != 0) {
                        changeRate = changeAmount.divide(previousValue, 4, BigDecimal.ROUND_HALF_UP)
                                        .multiply(BigDecimal.valueOf(100));
                }

                return ViewStatisticsDTO.ComparisonData.builder()
                                .previousValue(previousValue)
                                .currentValue(currentValue)
                                .changeAmount(changeAmount)
                                .changeRate(changeRate)
                                .isIncrease(changeAmount.compareTo(BigDecimal.ZERO) > 0)
                                .build();
        }

        /**
         * 조회 시점 계산 (오늘이면 현재 시간, 과거면 23:59:59)
         */
        private LocalDateTime getQueryDateTime(LocalDate targetDate) {
                if (targetDate.isEqual(LocalDate.now())) {
                        return LocalDateTime.now();
                } else {
                        return targetDate.atTime(23, 59, 59);
                }
        }

        /**
         * 특정 날짜의 원가 조회
         */
        private BigDecimal getCostPriceAtDate(Product product, LocalDate targetDate) {
                LocalDateTime queryTime = getQueryDateTime(targetDate);
                return productService.getProductPriceAtDate(product.getId(), queryTime)
                                .map(ProductPriceHistory::getCostPrice)
                                .orElse(BigDecimal.ZERO);
        }

        /**
         * 특정 날짜의 공급가 조회
         */
        private BigDecimal getSupplyPriceAtDate(Product product, LocalDate targetDate) {
                LocalDateTime queryTime = getQueryDateTime(targetDate);
                return productService.getProductPriceAtDate(product.getId(), queryTime)
                                .map(ProductPriceHistory::getSupplyPrice)
                                .orElse(BigDecimal.ZERO);
        }

        /**
         * 총 이익 계산
         */
        private BigDecimal calculateTotalProfit(List<DailySales> salesList, Long productId, LocalDateTime dateTime) {
                ProductPriceHistory priceHistory = productService.getProductPriceAtDate(productId, dateTime)
                                .orElse(null);

                if (priceHistory == null) {
                        return BigDecimal.ZERO;
                }

                int totalQuantity = salesList.stream().mapToInt(DailySales::getQuantity).sum();
                return PriceCalculator.calculateProfit(totalQuantity, priceHistory.getSupplyPrice(),
                                priceHistory.getCostPrice());
        }

        /**
         * 단일 이익 계산
         */
        private BigDecimal calculateSingleProfit(DailySales sales, Long productId, LocalDateTime dateTime) {
                if (sales == null) {
                        return BigDecimal.ZERO;
                }

                ProductPriceHistory priceHistory = productService.getProductPriceAtDate(productId, dateTime)
                                .orElse(null);

                if (priceHistory == null) {
                        return BigDecimal.ZERO;
                }

                return PriceCalculator.calculateProfit(sales.getQuantity(), priceHistory.getSupplyPrice(),
                                priceHistory.getCostPrice());
        }

        /**
         * 소계 생성
         */
        private ViewStatisticsDTO generateSubtotal(String category, List<ViewStatisticsDTO> categoryItems) {
                // 소계 계산 로직
                int dailyQty = categoryItems.stream().mapToInt(item -> item.getDailySummary().getQuantity()).sum();
                BigDecimal dailyAmount = categoryItems.stream()
                                .map(item -> item.getDailySummary().getAmount())
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal dailyProfit = categoryItems.stream()
                                .map(item -> item.getDailySummary().getProfit())
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                int monthlyQty = categoryItems.stream().mapToInt(item -> item.getMonthlySummary().getQuantity()).sum();
                BigDecimal monthlyAmount = categoryItems.stream()
                                .map(item -> item.getMonthlySummary().getAmount())
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal monthlyProfit = categoryItems.stream()
                                .map(item -> item.getMonthlySummary().getProfit())
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                int targetQty = categoryItems.stream().mapToInt(item -> item.getTargetData().getTargetQuantity()).sum();
                BigDecimal achievementRate = targetQty > 0
                                ? PriceCalculator.calculateAchievementRate(monthlyQty, targetQty)
                                : BigDecimal.ZERO;

                return ViewStatisticsDTO.builder()
                                .category("소계")
                                .productName(category + " 소계")
                                .dailySummary(ViewStatisticsDTO.DailySummary.builder()
                                                .quantity(dailyQty)
                                                .amount(dailyAmount)
                                                .profit(dailyProfit)
                                                .profitRate(PriceCalculator.calculateProfitRate(dailyProfit,
                                                                dailyAmount))
                                                .build())
                                .monthlySummary(ViewStatisticsDTO.MonthlySummary.builder()
                                                .quantity(monthlyQty)
                                                .amount(monthlyAmount)
                                                .profit(monthlyProfit)
                                                .profitRate(PriceCalculator.calculateProfitRate(monthlyProfit,
                                                                monthlyAmount))
                                                .build())
                                .targetData(ViewStatisticsDTO.TargetData.builder()
                                                .targetQuantity(targetQty)
                                                .achievementRate(achievementRate)
                                                .build())
                                .build();
        }

        /**
         * 전체 합계 생성
         */
        private ViewStatisticsDTO generateGrandTotal(List<ViewStatisticsDTO> allItems) {
                List<ViewStatisticsDTO> dataItems = allItems.stream()
                                .filter(item -> !"소계".equals(item.getCategory()) && !"합계".equals(item.getCategory()))
                                .collect(Collectors.toList());

                // 합계 계산 로직 (소계와 동일)
                int dailyQty = dataItems.stream().mapToInt(item -> item.getDailySummary().getQuantity()).sum();
                BigDecimal dailyAmount = dataItems.stream()
                                .map(item -> item.getDailySummary().getAmount())
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal dailyProfit = dataItems.stream()
                                .map(item -> item.getDailySummary().getProfit())
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                int monthlyQty = dataItems.stream().mapToInt(item -> item.getMonthlySummary().getQuantity()).sum();
                BigDecimal monthlyAmount = dataItems.stream()
                                .map(item -> item.getMonthlySummary().getAmount())
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal monthlyProfit = dataItems.stream()
                                .map(item -> item.getMonthlySummary().getProfit())
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                int targetQty = dataItems.stream().mapToInt(item -> item.getTargetData().getTargetQuantity()).sum();
                BigDecimal achievementRate = targetQty > 0
                                ? PriceCalculator.calculateAchievementRate(monthlyQty, targetQty)
                                : BigDecimal.ZERO;

                return ViewStatisticsDTO.builder()
                                .category("합계")
                                .productName("전체 합계")
                                .dailySummary(ViewStatisticsDTO.DailySummary.builder()
                                                .quantity(dailyQty)
                                                .amount(dailyAmount)
                                                .profit(dailyProfit)
                                                .profitRate(PriceCalculator.calculateProfitRate(dailyProfit,
                                                                dailyAmount))
                                                .build())
                                .monthlySummary(ViewStatisticsDTO.MonthlySummary.builder()
                                                .quantity(monthlyQty)
                                                .amount(monthlyAmount)
                                                .profit(monthlyProfit)
                                                .profitRate(PriceCalculator.calculateProfitRate(monthlyProfit,
                                                                monthlyAmount))
                                                .build())
                                .targetData(ViewStatisticsDTO.TargetData.builder()
                                                .targetQuantity(targetQty)
                                                .achievementRate(achievementRate)
                                                .build())
                                .build();
        }
}
