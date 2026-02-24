package com.mynet.sales_management_system.service;

import com.mynet.sales_management_system.dto.ProductComparisonDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class ProductComparisonExcelService {

    /**
     * 제품별 비교 엑셀 파일 생성
     */
    public byte[] generateProductComparisonExcel(
            ProductComparisonDTO comparisonData) throws IOException {

        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("제품별비교");

            // 스타일 생성
            Map<String, CellStyle> styles = createStyles(workbook);

            // 제목 행
            int currentRow = 0;
            currentRow = createTitle(sheet, styles, comparisonData.getProductInfo(), currentRow);

            // 헤더 작성
            currentRow = createHeaders(sheet, styles, comparisonData.getYears(), currentRow);

            // 데이터 작성
            currentRow = writeData(sheet, styles, comparisonData, currentRow);

            // 컬럼 너비 조정
            adjustColumnWidths(sheet);

            // 틀 고정
            sheet.createFreezePane(1, 3);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * 스타일 생성
     */
    private Map<String, CellStyle> createStyles(Workbook workbook) {
        Map<String, CellStyle> styles = new java.util.HashMap<>();

        // 폰트
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerFont.setFontHeightInPoints((short) 11);

        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        boldFont.setFontHeightInPoints((short) 10);

        Font greenFont = workbook.createFont();
        greenFont.setColor(IndexedColors.GREEN.getIndex());
        greenFont.setBold(true);

        Font redFont = workbook.createFont();
        redFont.setColor(IndexedColors.RED.getIndex());
        redFont.setBold(true);

        // 제목 스타일
        CellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        styles.put("title", titleStyle);

        // 헤더 스타일
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

        // 서브헤더
        CellStyle subHeaderStyle = workbook.createCellStyle();
        subHeaderStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        subHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        subHeaderStyle.setFont(boldFont);
        subHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
        subHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        subHeaderStyle.setBorderTop(BorderStyle.THIN);
        subHeaderStyle.setBorderBottom(BorderStyle.THIN);
        subHeaderStyle.setBorderLeft(BorderStyle.THIN);
        subHeaderStyle.setBorderRight(BorderStyle.THIN);
        styles.put("subHeader", subHeaderStyle);

        // 년도 셀 스타일
        CellStyle yearStyle = workbook.createCellStyle();
        yearStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        yearStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        yearStyle.setFont(boldFont);
        yearStyle.setAlignment(HorizontalAlignment.CENTER);
        yearStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        yearStyle.setBorderTop(BorderStyle.THIN);
        yearStyle.setBorderBottom(BorderStyle.THIN);
        yearStyle.setBorderLeft(BorderStyle.THIN);
        yearStyle.setBorderRight(BorderStyle.THIN);
        styles.put("year", yearStyle);

        // 현재년도 스타일
        CellStyle currentYearStyle = workbook.createCellStyle();
        currentYearStyle.cloneStyleFrom(yearStyle);
        currentYearStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        styles.put("currentYear", currentYearStyle);

        // 데이터 스타일
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setAlignment(HorizontalAlignment.RIGHT);
        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        styles.put("data", dataStyle);

        CellStyle numberStyle = workbook.createCellStyle();
        numberStyle.cloneStyleFrom(dataStyle);
        numberStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
        styles.put("number", numberStyle);

        // 월별 데이터 색상
        CellStyle qtyStyle = workbook.createCellStyle();
        qtyStyle.cloneStyleFrom(numberStyle);
        qtyStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        qtyStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put("qty", qtyStyle);

        CellStyle amtStyle = workbook.createCellStyle();
        amtStyle.cloneStyleFrom(numberStyle);
        amtStyle.setFillForegroundColor(IndexedColors.TAN.getIndex());
        amtStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put("amt", amtStyle);

        // 합계 스타일
        CellStyle totalQtyStyle = workbook.createCellStyle();
        totalQtyStyle.cloneStyleFrom(numberStyle);
        totalQtyStyle.setFont(boldFont);
        totalQtyStyle.setFillForegroundColor(IndexedColors.LIME.getIndex());
        totalQtyStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put("totalQty", totalQtyStyle);

        CellStyle totalAmtStyle = workbook.createCellStyle();
        totalAmtStyle.cloneStyleFrom(numberStyle);
        totalAmtStyle.setFont(boldFont);
        totalAmtStyle.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
        totalAmtStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put("totalAmt", totalAmtStyle);

        // 증감률 스타일
        CellStyle growthPositiveStyle = workbook.createCellStyle();
        growthPositiveStyle.cloneStyleFrom(numberStyle);
        growthPositiveStyle.setFont(greenFont);
        growthPositiveStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        growthPositiveStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put("growthPositive", growthPositiveStyle);

        CellStyle growthNegativeStyle = workbook.createCellStyle();
        growthNegativeStyle.cloneStyleFrom(numberStyle);
        growthNegativeStyle.setFont(redFont);
        growthNegativeStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        growthNegativeStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put("growthNegative", growthNegativeStyle);

        CellStyle growthZeroStyle = workbook.createCellStyle();
        growthZeroStyle.cloneStyleFrom(numberStyle);
        growthZeroStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        growthZeroStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put("growthZero", growthZeroStyle);

        return styles;
    }

    /**
     * 제목 행 작성
     */
    private int createTitle(Sheet sheet, Map<String, CellStyle> styles,
            ProductComparisonDTO.ProductInfo productInfo, int startRow) {
        Row titleRow = sheet.createRow(startRow++);
        titleRow.setHeightInPoints(25);

        String title = productInfo.isAllProducts()
                ? "제품별 비교 - 전체 제품"
                : "제품별 비교 - " + productInfo.getProductName();

        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(styles.get("title"));

        sheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 26));

        startRow++; // 여백
        return startRow;
    }

    /**
     * 헤더 작성
     */
    private int createHeaders(Sheet sheet, Map<String, CellStyle> styles,
            List<ProductComparisonDTO.YearData> years, int startRow) {
        // 첫 번째 헤더 행
        Row headerRow1 = sheet.createRow(startRow++);
        headerRow1.setHeightInPoints(20);

        int colIndex = 0;

        // 연도
        createCell(headerRow1, colIndex, "연도", styles.get("header"));
        sheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow, colIndex, colIndex));
        colIndex++;

        // 1월 ~ 12월 (각 2개씩)
        for (int month = 1; month <= 12; month++) {
            createCell(headerRow1, colIndex, month + "월", styles.get("header"));
            sheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, colIndex, colIndex + 1));
            colIndex += 2;
        }

        // 합계 (2개)
        createCell(headerRow1, colIndex, "합계", styles.get("header"));
        sheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, colIndex, colIndex + 1));

        // 두 번째 헤더 행
        Row headerRow2 = sheet.createRow(startRow++);
        headerRow2.setHeightInPoints(20);

        colIndex = 1; // 연도 열 건너뛰기

        // 1월 ~ 12월 상세
        for (int month = 1; month <= 12; month++) {
            createCell(headerRow2, colIndex++, "수량", styles.get("subHeader"));
            createCell(headerRow2, colIndex++, "금액", styles.get("subHeader"));
        }

        // 합계 상세
        createCell(headerRow2, colIndex++, "수량", styles.get("subHeader"));
        createCell(headerRow2, colIndex++, "금액", styles.get("subHeader"));

        return startRow;
    }

    /**
     * 데이터 작성
     */
    private int writeData(Sheet sheet, Map<String, CellStyle> styles,
            ProductComparisonDTO comparisonData, int startRow) {

        // 각 년도별 데이터
        for (ProductComparisonDTO.YearData yearData : comparisonData.getYears()) {
            Row dataRow = sheet.createRow(startRow++);
            int colIndex = 0;

            // 연도
            CellStyle yearStyle = yearData.isCurrentYear() ? styles.get("currentYear") : styles.get("year");
            createCell(dataRow, colIndex++, String.valueOf(yearData.getYear()), yearStyle);

            // 1월 ~ 12월 데이터
            for (ProductComparisonDTO.MonthData monthData : yearData.getMonths()) {
                createNumericCell(dataRow, colIndex++, monthData.getQuantity(), styles.get("qty"));
                createNumericCell(dataRow, colIndex++, monthData.getAmount(), styles.get("amt"));
            }

            // 연간 합계
            createNumericCell(dataRow, colIndex++, yearData.getYearTotal().getQuantity(), styles.get("totalQty"));
            createNumericCell(dataRow, colIndex++, yearData.getYearTotal().getAmount(), styles.get("totalAmt"));
        }

        // 증감률 행
        Row growthRow = sheet.createRow(startRow++);
        int colIndex = 0;

        createCell(growthRow, colIndex++, "증감률", styles.get("year"));

        // 월별 증감률
        for (BigDecimal rate : comparisonData.getGrowthRates().getMonthlyGrowthRates()) {
            createGrowthRateCell(growthRow, colIndex++, rate, styles);
            colIndex++; // 금액 열 건너뛰기 (증감률은 2개 병합)
            CellRangeAddress region = new CellRangeAddress(startRow - 1, startRow - 1, colIndex - 2, colIndex - 1);

            sheet.addMergedRegion(region);

            RegionUtil.setBorderTop(BorderStyle.THIN, region, sheet);
            RegionUtil.setBorderBottom(BorderStyle.THIN, region, sheet);
            RegionUtil.setBorderLeft(BorderStyle.THIN, region, sheet);
            RegionUtil.setBorderRight(BorderStyle.THIN, region, sheet);

        }

        // 전체 증감률
        int totalStartCol = colIndex;
        createGrowthRateCell(growthRow, colIndex++, comparisonData.getGrowthRates().getTotalGrowthRate(), styles);

        // 병합 (2칸)
        CellRangeAddress totalRegion = new CellRangeAddress(startRow - 1, startRow - 1, totalStartCol,
                totalStartCol + 1);

        sheet.addMergedRegion(totalRegion);

        RegionUtil.setBorderTop(BorderStyle.THIN, totalRegion, sheet);
        RegionUtil.setBorderBottom(BorderStyle.THIN, totalRegion, sheet);
        RegionUtil.setBorderLeft(BorderStyle.THIN, totalRegion, sheet);
        RegionUtil.setBorderRight(BorderStyle.THIN, totalRegion, sheet);

        colIndex++;

        return startRow;
    }

    /**
     * 증감률 셀 생성
     */
    private void createGrowthRateCell(Row row, int column, BigDecimal growthRate, Map<String, CellStyle> styles) {
        Cell cell = row.createCell(column);

        if (growthRate == null) {
            cell.setCellValue("0.0%");
            cell.setCellStyle(styles.get("growthZero"));
            return;
        }

        String value = String.format("%+.1f%%", growthRate.doubleValue());
        cell.setCellValue(value);

        if (growthRate.compareTo(BigDecimal.ZERO) > 0) {
            cell.setCellStyle(styles.get("growthPositive"));
        } else if (growthRate.compareTo(BigDecimal.ZERO) < 0) {
            cell.setCellStyle(styles.get("growthNegative"));
        } else {
            cell.setCellStyle(styles.get("growthZero"));
        }
    }

    /**
     * 컬럼 너비 조정
     */
    private void adjustColumnWidths(Sheet sheet) {
        sheet.setColumnWidth(0, 3000); // 연도

        // 1~12월 (각 2개씩)
        for (int i = 1; i <= 25; i++) {
            sheet.setColumnWidth(i, 3500);
        }

        sheet.setColumnWidth(25, 4000); // 합계 수량
        sheet.setColumnWidth(26, 4500); // 합계 금액
    }

    private Cell createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
        return cell;
    }

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