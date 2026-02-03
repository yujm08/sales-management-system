package com.mynet.sales_management_system.service;

import com.mynet.sales_management_system.dto.DailySalesStatusDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class DailySalesExcelService {

    private static final String[] COMPANIES = {
            "영현아이앤씨", "마이씨앤에스", "우리STM", "엠에스앤샵",
            "원이스토리(쿠팡)", "대현씨앤씨", "마이넷(GX판매)"
    };

    /**
     * 일일매출현황 엑셀 파일 생성
     */
    public byte[] generateDailySalesExcel(
            Map<String, List<DailySalesStatusDTO>> dailySalesData,
            LocalDate targetDate) throws IOException {

        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("일일매출현황");

            // 스타일 생성
            Map<String, CellStyle> styles = createStyles(workbook);

            // 헤더 작성
            int currentRow = 0;
            currentRow = createHeaders(sheet, styles, targetDate, currentRow);

            // 데이터 작성
            currentRow = writeData(sheet, styles, dailySalesData, currentRow);

            // 컬럼 너비 자동 조정
            adjustColumnWidths(sheet);

            // 틀 고정: A~C열(0,1,2) 고정, 즉 D1(3,0)부터 스크롤
            // 3열 이후부터 스크롤, 2행 이후부터 스크롤 (헤더 2행 고정)
            sheet.createFreezePane(3, 2);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * 스타일 생성
     */
    private Map<String, CellStyle> createStyles(Workbook workbook) {
        Map<String, CellStyle> styles = new java.util.HashMap<>();

        // 폰트 생성
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerFont.setFontHeightInPoints((short) 10);

        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        boldFont.setFontHeightInPoints((short) 10);

        Font whiteFont = workbook.createFont();
        whiteFont.setBold(true);
        whiteFont.setColor(IndexedColors.WHITE.getIndex());
        whiteFont.setFontHeightInPoints((short) 10);

        // 헤더 스타일 (어두운 청록색)
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        styles.put("header", headerStyle);

        // 서브헤더 스타일 (회색)
        CellStyle subHeaderStyle = workbook.createCellStyle();
        subHeaderStyle.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
        subHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        subHeaderStyle.setFont(whiteFont);
        subHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
        subHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        subHeaderStyle.setBorderTop(BorderStyle.THIN);
        subHeaderStyle.setBorderBottom(BorderStyle.THIN);
        subHeaderStyle.setBorderLeft(BorderStyle.THIN);
        subHeaderStyle.setBorderRight(BorderStyle.THIN);
        styles.put("subHeader", subHeaderStyle);

        // 카테고리 셀 스타일
        CellStyle categoryStyle = workbook.createCellStyle();
        categoryStyle.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
        categoryStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        categoryStyle.setFont(whiteFont);
        categoryStyle.setAlignment(HorizontalAlignment.CENTER);
        categoryStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        categoryStyle.setBorderTop(BorderStyle.THIN);
        categoryStyle.setBorderBottom(BorderStyle.THIN);
        categoryStyle.setBorderLeft(BorderStyle.THIN);
        categoryStyle.setBorderRight(BorderStyle.THIN);
        styles.put("category", categoryStyle);

        // 데이터 셀 스타일 (기본 - 흰색)
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setAlignment(HorizontalAlignment.RIGHT);
        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        styles.put("data", dataStyle);

        // 제품 정보 스타일 (좌측 정렬 + 흰색)
        CellStyle productStyle = workbook.createCellStyle();
        productStyle.cloneStyleFrom(dataStyle);
        productStyle.setAlignment(HorizontalAlignment.LEFT);
        styles.put("product", productStyle);

        // 숫자 포맷 스타일 (기본)
        CellStyle numberStyle = workbook.createCellStyle();
        numberStyle.cloneStyleFrom(dataStyle);
        numberStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
        styles.put("number", numberStyle);

        // ===== 헤더용 특수 열 스타일 (색상 있음) =====

        // 일 합계 헤더 스타일 (연한 파란색)
        CellStyle dailyTotalHeaderStyle = workbook.createCellStyle();
        dailyTotalHeaderStyle.cloneStyleFrom(numberStyle);
        dailyTotalHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
        dailyTotalHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        dailyTotalHeaderStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        dailyTotalHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put("dailyTotalHeader", dailyTotalHeaderStyle);

        // 월 합계 헤더 스타일 (연한 주황색)
        CellStyle monthlyTotalHeaderStyle = workbook.createCellStyle();
        monthlyTotalHeaderStyle.cloneStyleFrom(numberStyle);
        monthlyTotalHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
        monthlyTotalHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        monthlyTotalHeaderStyle.setFillForegroundColor(IndexedColors.TAN.getIndex());
        monthlyTotalHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put("monthlyTotalHeader", monthlyTotalHeaderStyle);

        // 목표 헤더 스타일 (노란색)
        CellStyle targetHeaderStyle = workbook.createCellStyle();
        targetHeaderStyle.cloneStyleFrom(numberStyle);
        targetHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
        targetHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        targetHeaderStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        targetHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put("targetHeader", targetHeaderStyle);

        // 비교1 헤더 스타일 (연한 녹색)
        CellStyle comparison1HeaderStyle = workbook.createCellStyle();
        comparison1HeaderStyle.cloneStyleFrom(numberStyle);
        comparison1HeaderStyle.setAlignment(HorizontalAlignment.CENTER);
        comparison1HeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        comparison1HeaderStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        comparison1HeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put("comparison1Header", comparison1HeaderStyle);

        // 비교2 헤더 스타일 (연한 보라색)
        CellStyle comparison2HeaderStyle = workbook.createCellStyle();
        comparison2HeaderStyle.cloneStyleFrom(numberStyle);
        comparison2HeaderStyle.setAlignment(HorizontalAlignment.CENTER);
        comparison2HeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        comparison2HeaderStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        comparison2HeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put("comparison2Header", comparison2HeaderStyle);

        // ===== 소계 스타일 (회색 배경 + 검은 글씨) =====
        CellStyle subtotalStyle = workbook.createCellStyle();
        subtotalStyle.cloneStyleFrom(numberStyle);
        subtotalStyle.setFont(boldFont);
        subtotalStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        subtotalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put("subtotal", subtotalStyle);

        // 소계 - 특수 열용 (색상 있음)
        CellStyle subtotalDailyStyle = workbook.createCellStyle();
        subtotalDailyStyle.cloneStyleFrom(subtotalStyle);
        subtotalDailyStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        subtotalDailyStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put("subtotalDaily", subtotalDailyStyle);

        CellStyle subtotalMonthlyStyle = workbook.createCellStyle();
        subtotalMonthlyStyle.cloneStyleFrom(subtotalStyle);
        subtotalMonthlyStyle.setFillForegroundColor(IndexedColors.TAN.getIndex());
        subtotalMonthlyStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put("subtotalMonthly", subtotalMonthlyStyle);

        CellStyle subtotalTargetStyle = workbook.createCellStyle();
        subtotalTargetStyle.cloneStyleFrom(subtotalStyle);
        subtotalTargetStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        subtotalTargetStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put("subtotalTarget", subtotalTargetStyle);

        CellStyle subtotalComparison1Style = workbook.createCellStyle();
        subtotalComparison1Style.cloneStyleFrom(subtotalStyle);
        subtotalComparison1Style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        subtotalComparison1Style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put("subtotalComparison1", subtotalComparison1Style);

        CellStyle subtotalComparison2Style = workbook.createCellStyle();
        subtotalComparison2Style.cloneStyleFrom(subtotalStyle);
        subtotalComparison2Style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        subtotalComparison2Style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put("subtotalComparison2", subtotalComparison2Style);

        // ===== 합계 스타일 (진한 회색 배경 + 흰 글씨) =====
        CellStyle totalStyle = workbook.createCellStyle();
        totalStyle.cloneStyleFrom(numberStyle);
        totalStyle.setFont(whiteFont);
        totalStyle.setFillForegroundColor(IndexedColors.GREY_80_PERCENT.getIndex());
        totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put("total", totalStyle);

        // 합계 - 특수 열용 (진한 회색 + 흰 글씨)
        CellStyle totalDailyStyle = workbook.createCellStyle();
        totalDailyStyle.cloneStyleFrom(totalStyle);
        totalDailyStyle.setFillForegroundColor(IndexedColors.GREY_80_PERCENT.getIndex());
        totalDailyStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put("totalDaily", totalDailyStyle);

        CellStyle totalMonthlyStyle = workbook.createCellStyle();
        totalMonthlyStyle.cloneStyleFrom(totalStyle);
        styles.put("totalMonthly", totalMonthlyStyle);

        CellStyle totalTargetStyle = workbook.createCellStyle();
        totalTargetStyle.cloneStyleFrom(totalStyle);
        styles.put("totalTarget", totalTargetStyle);

        CellStyle totalComparison1Style = workbook.createCellStyle();
        totalComparison1Style.cloneStyleFrom(totalStyle);
        styles.put("totalComparison1", totalComparison1Style);

        CellStyle totalComparison2Style = workbook.createCellStyle();
        totalComparison2Style.cloneStyleFrom(totalStyle);
        styles.put("totalComparison2", totalComparison2Style);

        return styles;
    }

    /**
     * 헤더 작성
     */
    private int createHeaders(Sheet sheet, Map<String, CellStyle> styles, LocalDate targetDate, int startRow) {
        // 첫 번째 헤더 행
        Row headerRow1 = sheet.createRow(startRow++);
        headerRow1.setHeightInPoints(25);

        int colIndex = 0;

        // 고정 컬럼 (구분, 제품, 공급가)
        createCell(headerRow1, colIndex++, "구분", styles.get("header"));
        createCell(headerRow1, colIndex++, "제품", styles.get("header"));
        createCell(headerRow1, colIndex++, "공급가", styles.get("header"));

        // 회사별 컬럼 (7개 회사)
        for (String company : COMPANIES) {
            createCell(headerRow1, colIndex, company, styles.get("header"));
            sheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, colIndex, colIndex + 2));
            colIndex += 3;
        }

        // 일 합계 (파란색)
        createCell(headerRow1, colIndex, "일 합계", styles.get("dailyTotalHeader"));
        sheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, colIndex, colIndex + 1));
        colIndex += 2;

        // 월 합계 (주황색)
        createCell(headerRow1, colIndex, "월 합계", styles.get("monthlyTotalHeader"));
        sheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, colIndex, colIndex + 1));
        colIndex += 2;

        // 목표 및 비교 컬럼 (노란색, 녹색, 보라색)
        String monthStr = targetDate.format(DateTimeFormatter.ofPattern("M"));
        createCell(headerRow1, colIndex++, monthStr + "월 목표수량", styles.get("targetHeader"));
        createCell(headerRow1, colIndex++, "전월 판매량", styles.get("comparison1Header"));

        String prevMonth = targetDate.minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy.M"));
        String currentMonth = targetDate.format(DateTimeFormatter.ofPattern("yyyy.M"));
        createCell(headerRow1, colIndex++, prevMonth + " - " + currentMonth + " 판매량", styles.get("comparison1Header"));

        String lastYearPrevMonth = targetDate.minusYears(1).minusMonths(1)
                .format(DateTimeFormatter.ofPattern("yyyy년 M월"));
        createCell(headerRow1, colIndex++, lastYearPrevMonth + " 판매량", styles.get("comparison2Header"));

        String lastYearCurrentMonth = targetDate.minusYears(1).format(DateTimeFormatter.ofPattern("yyyy년 M월"));
        createCell(headerRow1, colIndex++, lastYearCurrentMonth + " 판매량", styles.get("comparison2Header"));

        String lastYearPrevToCurrentMonth = targetDate.minusYears(1).minusMonths(1)
                .format(DateTimeFormatter.ofPattern("yyyy.M"))
                + " - " + targetDate.minusYears(1).format(DateTimeFormatter.ofPattern("yyyy.M"));
        createCell(headerRow1, colIndex++, lastYearPrevToCurrentMonth + " 판매량", styles.get("comparison2Header"));

        // 두 번째 헤더 행 (서브헤더)
        Row headerRow2 = sheet.createRow(startRow++);
        headerRow2.setHeightInPoints(20);

        colIndex = 0;

        // 고정 컬럼은 첫 행과 병합
        sheet.addMergedRegion(new CellRangeAddress(startRow - 2, startRow - 1, 0, 0)); // 구분
        sheet.addMergedRegion(new CellRangeAddress(startRow - 2, startRow - 1, 1, 1)); // 제품
        sheet.addMergedRegion(new CellRangeAddress(startRow - 2, startRow - 1, 2, 2)); // 공급가
        colIndex = 3;

        // 회사별 서브헤더 (7회 반복) - 회색
        for (int i = 0; i < 7; i++) {
            createCell(headerRow2, colIndex++, "일 수량", styles.get("subHeader"));
            createCell(headerRow2, colIndex++, "월 수량", styles.get("subHeader"));
            createCell(headerRow2, colIndex++, "금액", styles.get("subHeader"));
        }

        // 일 합계 서브헤더 (파란색)
        createCell(headerRow2, colIndex++, "수량", styles.get("dailyTotalHeader"));
        createCell(headerRow2, colIndex++, "금액", styles.get("dailyTotalHeader"));

        // 월 합계 서브헤더 (주황색)
        createCell(headerRow2, colIndex++, "수량", styles.get("monthlyTotalHeader"));
        createCell(headerRow2, colIndex++, "금액", styles.get("monthlyTotalHeader"));

        // 비교 컬럼은 첫 행과 병합
        for (int i = 0; i < 6; i++) {
            sheet.addMergedRegion(new CellRangeAddress(startRow - 2, startRow - 1, colIndex, colIndex));
            colIndex++;
        }

        // 병합 후, 비어 있는 셀 스타일 채우기
        for (int c = 0; c < sheet.getRow(startRow - 2).getLastCellNum(); c++) {
            if (headerRow2.getCell(c) == null) {
                Cell cell = headerRow2.createCell(c);
                cell.setCellStyle(styles.get("subHeader"));
            }
        }

        return startRow;
    }

    /**
     * 데이터 작성
     */
    private int writeData(Sheet sheet, Map<String, CellStyle> styles,
            Map<String, List<DailySalesStatusDTO>> dailySalesData, int startRow) {

        int currentRow = startRow;
        int categoryRowStart;

        // 카테고리별 순회
        for (Map.Entry<String, List<DailySalesStatusDTO>> categoryEntry : dailySalesData.entrySet()) {
            String category = categoryEntry.getKey();
            List<DailySalesStatusDTO> products = categoryEntry.getValue();

            categoryRowStart = currentRow;

            // 제품별 데이터 작성
            for (DailySalesStatusDTO product : products) {
                Row dataRow = sheet.createRow(currentRow++);
                int colIndex = 0;

                // 구분 (카테고리) - 첫 행에만
                if (currentRow - 1 == categoryRowStart) {
                    Cell categoryCell = createCell(dataRow, colIndex, category, styles.get("category"));
                    // 병합은 소계 작성 후에
                }
                colIndex++;

                // 제품 정보
                createCell(dataRow, colIndex++, product.getProductName(),
                        styles.get("product"));

                // 공급가
                createNumericCell(dataRow, colIndex++, product.getSupplyPrice(), styles.get("number"));

                // 회사별 데이터 (7개 회사 * 3컬럼) - 연한 녹색
                for (String company : COMPANIES) {
                    var companySales = product.getCompanySalesMap().get(company);
                    if (companySales != null) {
                        createNumericCell(dataRow, colIndex++, companySales.getDailyQuantity(), styles.get("number"));
                        createNumericCell(dataRow, colIndex++, companySales.getMonthlyQuantity(), styles.get("number"));
                        createNumericCell(dataRow, colIndex++, companySales.getAmount().longValue(),
                                styles.get("number"));
                    } else {
                        createCell(dataRow, colIndex++, "-", styles.get("data"));
                        createCell(dataRow, colIndex++, "-", styles.get("data"));
                        createCell(dataRow, colIndex++, "-", styles.get("data"));
                    }
                }

                // 일 합계 (흰색 - 제품 행에는 색상 없음)
                createNumericCell(dataRow, colIndex++, product.getDailyTotal().getQuantity(), styles.get("number"));
                createNumericCell(dataRow, colIndex++, product.getDailyTotal().getAmount().longValue(),
                        styles.get("number"));

                // 월 합계 (흰색 - 제품 행에는 색상 없음)
                createNumericCell(dataRow, colIndex++, product.getMonthlyTotal().getQuantity(), styles.get("number"));
                createNumericCell(dataRow, colIndex++, product.getMonthlyTotal().getAmount().longValue(),
                        styles.get("number"));

                // 비교 데이터 (흰색 - 제품 행에는 색상 없음)
                createNumericCell(dataRow, colIndex++, product.getTargetQuantity(), styles.get("number"));
                createNumericCell(dataRow, colIndex++, product.getPrevMonthSales(), styles.get("number"));
                createNumericCell(dataRow, colIndex++, product.getYearToDateSales(), styles.get("number"));
                createNumericCell(dataRow, colIndex++, product.getLastYearPrevMonthSales(), styles.get("number"));
                createNumericCell(dataRow, colIndex++, product.getLastYearCurrentMonthSales(), styles.get("number"));
                createNumericCell(dataRow, colIndex++, product.getLastYearYearToDateSales(), styles.get("number"));
            }

            // 카테고리 컬럼 병합
            if (products.size() > 1) {
                sheet.addMergedRegion(new CellRangeAddress(categoryRowStart, currentRow - 1, 0, 0));
            }

            // 소계 행 작성
            currentRow = writeSubtotal(sheet, styles, products, currentRow, category);
        }

        // 전체 합계 행 작성
        currentRow = writeTotal(sheet, styles, dailySalesData, currentRow);

        return currentRow;
    }

    /**
     * 소계 행 작성
     */
    private int writeSubtotal(Sheet sheet, Map<String, CellStyle> styles,
            List<DailySalesStatusDTO> products, int currentRow, String category) {
        Row subtotalRow = sheet.createRow(currentRow++);
        int colIndex = 0;

        // A~C 병합 (0~2)
        sheet.addMergedRegion(
                new CellRangeAddress(
                        subtotalRow.getRowNum(), // start row
                        subtotalRow.getRowNum(), // end row
                        0, // start col (A)
                        2 // end col (C)
                ));

        // 병합된 A셀에 "소계" 표시
        Cell subtotalLabelCell = createCell(
                subtotalRow,
                0,
                "소계",
                styles.get("subtotal"));

        // 가운데 정렬 강제 (안전장치)
        subtotalLabelCell.getCellStyle().setAlignment(HorizontalAlignment.CENTER);
        subtotalLabelCell.getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);

        // 병합 영역 전체에 테두리 강제 적용
        for (int c = 0; c <= 2; c++) {
            Cell cell = subtotalRow.getCell(c);
            if (cell == null) {
                cell = subtotalRow.createCell(c);
            }
            cell.setCellStyle(styles.get("subtotal"));
        }

        // colIndex를 3으로 점프
        colIndex = 3;

        // 회사별 소계 계산 (7개 회사 * 3컬럼) - 흰색 배경
        for (String company : COMPANIES) {
            long dailyQty = products.stream()
                    .filter(p -> p.getCompanySalesMap().containsKey(company))
                    .mapToLong(p -> p.getCompanySalesMap().get(company).getDailyQuantity())
                    .sum();
            long monthlyQty = products.stream()
                    .filter(p -> p.getCompanySalesMap().containsKey(company))
                    .mapToLong(p -> p.getCompanySalesMap().get(company).getMonthlyQuantity())
                    .sum();
            long amount = products.stream()
                    .filter(p -> p.getCompanySalesMap().containsKey(company))
                    .mapToLong(p -> p.getCompanySalesMap().get(company).getAmount().longValue())
                    .sum();

            createNumericCell(subtotalRow, colIndex++, dailyQty, styles.get("subtotal"));
            createNumericCell(subtotalRow, colIndex++, monthlyQty, styles.get("subtotal"));
            createNumericCell(subtotalRow, colIndex++, amount, styles.get("subtotal"));
        }

        // 일 합계 (파란색)
        long dailyTotalQty = products.stream().mapToLong(p -> p.getDailyTotal().getQuantity()).sum();
        long dailyTotalAmt = products.stream().mapToLong(p -> p.getDailyTotal().getAmount().longValue()).sum();
        createNumericCell(subtotalRow, colIndex++, dailyTotalQty, styles.get("subtotalDaily"));
        createNumericCell(subtotalRow, colIndex++, dailyTotalAmt, styles.get("subtotalDaily"));

        // 월 합계 (주황색)
        long monthlyTotalQty = products.stream().mapToLong(p -> p.getMonthlyTotal().getQuantity()).sum();
        long monthlyTotalAmt = products.stream().mapToLong(p -> p.getMonthlyTotal().getAmount().longValue()).sum();
        createNumericCell(subtotalRow, colIndex++, monthlyTotalQty, styles.get("subtotalMonthly"));
        createNumericCell(subtotalRow, colIndex++, monthlyTotalAmt, styles.get("subtotalMonthly"));

        // 비교 데이터
        long targetQty = products.stream().mapToLong(DailySalesStatusDTO::getTargetQuantity).sum();
        long prevMonth = products.stream().mapToLong(DailySalesStatusDTO::getPrevMonthSales).sum();
        long ytd = products.stream().mapToLong(DailySalesStatusDTO::getYearToDateSales).sum();
        long lyPrevMonth = products.stream().mapToLong(DailySalesStatusDTO::getLastYearPrevMonthSales).sum();
        long lyCurrentMonth = products.stream().mapToLong(DailySalesStatusDTO::getLastYearCurrentMonthSales).sum();
        long lyYtd = products.stream().mapToLong(DailySalesStatusDTO::getLastYearYearToDateSales).sum();

        createNumericCell(subtotalRow, colIndex++, targetQty, styles.get("subtotalTarget"));
        createNumericCell(subtotalRow, colIndex++, prevMonth, styles.get("subtotalComparison1"));
        createNumericCell(subtotalRow, colIndex++, ytd, styles.get("subtotalComparison1"));
        createNumericCell(subtotalRow, colIndex++, lyPrevMonth, styles.get("subtotalComparison2"));
        createNumericCell(subtotalRow, colIndex++, lyCurrentMonth, styles.get("subtotalComparison2"));
        createNumericCell(subtotalRow, colIndex++, lyYtd, styles.get("subtotalComparison2"));

        return currentRow;
    }

    /**
     * 전체 합계 행 작성
     */
    private int writeTotal(Sheet sheet, Map<String, CellStyle> styles,
            Map<String, List<DailySalesStatusDTO>> dailySalesData, int currentRow) {
        Row totalRow = sheet.createRow(currentRow++);
        int colIndex = 0;

        // 모든 제품 리스트
        List<DailySalesStatusDTO> allProducts = dailySalesData.values().stream()
                .flatMap(List::stream)
                .toList();

        // 카테고리 셀 (빈칸) - 진한 회색 + 흰 글씨
        createCell(totalRow, colIndex++, "", styles.get("total"));

        // "전체 합계" 표시 - 진한 회색 + 흰 글씨
        createCell(totalRow, colIndex++, "전체 합계", styles.get("total"));

        // 공급가 (빈칸) - 진한 회색
        createCell(totalRow, colIndex++, "", styles.get("total"));

        // 회사별 합계 (7개 회사 * 3컬럼) - 진한 회색
        for (String company : COMPANIES) {
            long dailyQty = allProducts.stream()
                    .filter(p -> p.getCompanySalesMap().containsKey(company))
                    .mapToLong(p -> p.getCompanySalesMap().get(company).getDailyQuantity())
                    .sum();
            long monthlyQty = allProducts.stream()
                    .filter(p -> p.getCompanySalesMap().containsKey(company))
                    .mapToLong(p -> p.getCompanySalesMap().get(company).getMonthlyQuantity())
                    .sum();
            long amount = allProducts.stream()
                    .filter(p -> p.getCompanySalesMap().containsKey(company))
                    .mapToLong(p -> p.getCompanySalesMap().get(company).getAmount().longValue())
                    .sum();

            createNumericCell(totalRow, colIndex++, dailyQty, styles.get("total"));
            createNumericCell(totalRow, colIndex++, monthlyQty, styles.get("total"));
            createNumericCell(totalRow, colIndex++, amount, styles.get("total"));
        }

        // 일 합계 - 진한 회색
        long dailyTotalQty = allProducts.stream().mapToLong(p -> p.getDailyTotal().getQuantity()).sum();
        long dailyTotalAmt = allProducts.stream().mapToLong(p -> p.getDailyTotal().getAmount().longValue()).sum();
        createNumericCell(totalRow, colIndex++, dailyTotalQty, styles.get("totalDaily"));
        createNumericCell(totalRow, colIndex++, dailyTotalAmt, styles.get("totalDaily"));

        // 월 합계 - 진한 회색
        long monthlyTotalQty = allProducts.stream().mapToLong(p -> p.getMonthlyTotal().getQuantity()).sum();
        long monthlyTotalAmt = allProducts.stream().mapToLong(p -> p.getMonthlyTotal().getAmount().longValue()).sum();
        createNumericCell(totalRow, colIndex++, monthlyTotalQty, styles.get("totalMonthly"));
        createNumericCell(totalRow, colIndex++, monthlyTotalAmt, styles.get("totalMonthly"));

        // 비교 데이터 - 진한 회색
        long targetQty = allProducts.stream().mapToLong(DailySalesStatusDTO::getTargetQuantity).sum();
        long prevMonth = allProducts.stream().mapToLong(DailySalesStatusDTO::getPrevMonthSales).sum();
        long ytd = allProducts.stream().mapToLong(DailySalesStatusDTO::getYearToDateSales).sum();
        long lyPrevMonth = allProducts.stream().mapToLong(DailySalesStatusDTO::getLastYearPrevMonthSales).sum();
        long lyCurrentMonth = allProducts.stream().mapToLong(DailySalesStatusDTO::getLastYearCurrentMonthSales).sum();
        long lyYtd = allProducts.stream().mapToLong(DailySalesStatusDTO::getLastYearYearToDateSales).sum();

        createNumericCell(totalRow, colIndex++, targetQty, styles.get("totalTarget"));
        createNumericCell(totalRow, colIndex++, prevMonth, styles.get("totalComparison1"));
        createNumericCell(totalRow, colIndex++, ytd, styles.get("totalComparison1"));
        createNumericCell(totalRow, colIndex++, lyPrevMonth, styles.get("totalComparison2"));
        createNumericCell(totalRow, colIndex++, lyCurrentMonth, styles.get("totalComparison2"));
        createNumericCell(totalRow, colIndex++, lyYtd, styles.get("totalComparison2"));

        return currentRow;
    }

    /**
     * 컬럼 너비 자동 조정
     */
    private void adjustColumnWidths(Sheet sheet) {
        // 구분, 제품, 공급가
        sheet.setColumnWidth(0, 2500); // 구분
        sheet.setColumnWidth(1, 2200); // 제품
        sheet.setColumnWidth(2, 2500); // 공급가

        // 나머지 컬럼 (회사별 + 합계 + 비교)
        int totalColumns = 3 + (7 * 3) + 4 + 6;
        for (int i = 3; i < totalColumns; i++) {
            sheet.setColumnWidth(i, 3000);
        }

        // 비교/판매량 컬럼은 넓게
        sheet.setColumnWidth(28, 3200); // AE
        sheet.setColumnWidth(30, 6000); // AE
        sheet.setColumnWidth(31, 5000); // AF
        sheet.setColumnWidth(32, 5000); // AG
        sheet.setColumnWidth(33, 6000); // AH
    }

    /**
     * 셀 생성 (문자열)
     */
    private Cell createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
        return cell;
    }

    /**
     * 셀 생성 (숫자)
     */
    private Cell createNumericCell(Row row, int column, Number value, CellStyle style) {
        Cell cell = row.createCell(column);

        if (value != null && value.longValue() != 0) {
            cell.setCellValue(value.longValue());
        } else {
            cell.setCellValue("-");
        }

        cell.setCellStyle(style);
        return cell;
    }
}