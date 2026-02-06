package com.mynet.sales_management_system.service;

import com.mynet.sales_management_system.dto.YearlyComparisonDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class YearlyComparisonExcelService {

    /**
     * 년도별 비교 엑셀 파일 생성
     */
    public byte[] generateYearlyComparisonExcel(
            List<YearlyComparisonDTO.CategoryData> categories,
            YearlyComparisonDTO.GrandTotal grandTotal,
            int year1, int year2, int year3) throws IOException {

        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("년도별비교");

            // 스타일 생성
            Map<String, CellStyle> styles = createStyles(workbook);

            // 헤더 작성
            int currentRow = 0;
            currentRow = createHeaders(sheet, styles, year1, year2, year3, currentRow);

            // 데이터 작성
            currentRow = writeData(sheet, styles, categories, currentRow);

            // 합계 행 작성
            if (grandTotal != null) {
                currentRow = writeGrandTotal(sheet, styles, grandTotal, currentRow);
            }

            // 컬럼 너비 조정
            adjustColumnWidths(sheet);

            // 틀 고정
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

        // 폰트
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerFont.setFontHeightInPoints((short) 11);

        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        boldFont.setFontHeightInPoints((short) 10);

        Font whiteFont = workbook.createFont();
        whiteFont.setBold(true);
        whiteFont.setColor(IndexedColors.WHITE.getIndex());
        whiteFont.setFontHeightInPoints((short) 10);

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

        // 카테고리 스타일
        CellStyle categoryStyle = workbook.createCellStyle();
        categoryStyle.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
        categoryStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        categoryStyle.setFont(whiteFont);
        categoryStyle.setAlignment(HorizontalAlignment.CENTER);
        categoryStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        categoryStyle.setBorderTop(BorderStyle.THIN);
        categoryStyle.setBorderBottom(BorderStyle.THIN);
        categoryStyle.setBorderLeft(BorderStyle.THIN);
        categoryStyle.setBorderRight(BorderStyle.THIN);
        styles.put("category", categoryStyle);

        // 데이터 스타일
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setAlignment(HorizontalAlignment.RIGHT);
        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        styles.put("data", dataStyle);

        CellStyle productStyle = workbook.createCellStyle();
        productStyle.cloneStyleFrom(dataStyle);
        productStyle.setAlignment(HorizontalAlignment.LEFT);
        styles.put("product", productStyle);

        CellStyle numberStyle = workbook.createCellStyle();
        numberStyle.cloneStyleFrom(dataStyle);
        numberStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
        styles.put("number", numberStyle);

        // 카테고리별 배경색
        String[] colorNames = { "green1", "green2", "green3", "green4" };
        for (int i = 0; i < 4; i++) {
            CellStyle colorStyle = workbook.createCellStyle();
            colorStyle.cloneStyleFrom(numberStyle);
            colorStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            colorStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            styles.put(colorNames[i], colorStyle);
        }

        // 증감률 스타일
        Font greenFont = workbook.createFont();
        greenFont.setColor(IndexedColors.GREEN.getIndex());
        greenFont.setBold(true);

        Font redFont = workbook.createFont();
        redFont.setColor(IndexedColors.RED.getIndex());
        redFont.setBold(true);

        CellStyle growthPositiveStyle = workbook.createCellStyle();
        growthPositiveStyle.cloneStyleFrom(numberStyle);
        growthPositiveStyle.setFont(greenFont);
        styles.put("growthPositive", growthPositiveStyle);

        CellStyle growthNegativeStyle = workbook.createCellStyle();
        growthNegativeStyle.cloneStyleFrom(numberStyle);
        growthNegativeStyle.setFont(redFont);
        styles.put("growthNegative", growthNegativeStyle);

        // 합계 행
        CellStyle grandTotalStyle = workbook.createCellStyle();
        grandTotalStyle.cloneStyleFrom(numberStyle);
        grandTotalStyle.setFont(boldFont);
        grandTotalStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        grandTotalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put("grandTotal", grandTotalStyle);

        return styles;
    }

    /**
     * 헤더 작성
     */
    private int createHeaders(Sheet sheet, Map<String, CellStyle> styles,
            int year1, int year2, int year3, int startRow) {
        // 첫 번째 헤더 행
        Row headerRow1 = sheet.createRow(startRow++);
        headerRow1.setHeightInPoints(20);

        int colIndex = 0;

        // 제품 (3개 병합)
        createCell(headerRow1, colIndex, "제품", styles.get("header"));
        sheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, colIndex, colIndex + 2));
        colIndex += 3;

        // 매출 (3개 병합)
        createCell(headerRow1, colIndex, "매출", styles.get("header"));
        sheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, colIndex, colIndex + 2));
        colIndex += 3;

        // 증감률
        createCell(headerRow1, colIndex, "증감률", styles.get("header"));
        sheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow, colIndex, colIndex));

        // 두 번째 헤더 행
        Row headerRow2 = sheet.createRow(startRow++);
        headerRow2.setHeightInPoints(20);

        colIndex = 0;

        // 제품 상세
        createCell(headerRow2, colIndex++, "제품분류", styles.get("subHeader"));
        createCell(headerRow2, colIndex++, "제품코드", styles.get("subHeader"));
        createCell(headerRow2, colIndex++, "제품명", styles.get("subHeader"));

        // 년도
        createCell(headerRow2, colIndex++, String.valueOf(year1), styles.get("subHeader"));
        createCell(headerRow2, colIndex++, String.valueOf(year2), styles.get("subHeader"));
        createCell(headerRow2, colIndex++, String.valueOf(year3), styles.get("subHeader"));

        return startRow;
    }

    /**
     * 데이터 작성
     */
    private int writeData(Sheet sheet, Map<String, CellStyle> styles,
            List<YearlyComparisonDTO.CategoryData> categories,
            int startRow) {

        int currentRow = startRow;
        int categoryColorIndex = 0;

        for (YearlyComparisonDTO.CategoryData categoryData : categories) {
            String colorStyleName = "green" + ((categoryColorIndex % 4) + 1);
            categoryColorIndex++;

            List<YearlyComparisonDTO.ProductYearlyData> products = categoryData.getProducts();
            int categoryRowStart = currentRow;

            for (YearlyComparisonDTO.ProductYearlyData product : products) {
                Row dataRow = sheet.createRow(currentRow++);
                int colIndex = 0;

                // 카테고리
                if (currentRow - 1 == categoryRowStart) {
                    createCell(dataRow, colIndex, categoryData.getCategory(), styles.get("category"));
                }
                colIndex++;

                // 제품코드
                createCell(dataRow, colIndex++, product.getProductCode(), styles.get("data"));

                // 제품명
                createCell(dataRow, colIndex++, product.getProductName(), styles.get("product"));

                // 년도별 금액
                createNumericCell(dataRow, colIndex++, product.getYear1Amount(), styles.get(colorStyleName));
                createNumericCell(dataRow, colIndex++, product.getYear2Amount(), styles.get(colorStyleName));
                createNumericCell(dataRow, colIndex++, product.getYear3Amount(), styles.get(colorStyleName));

                // 증감률
                createGrowthRateCell(dataRow, colIndex++, product.getGrowthRate(), styles);
            }

            // 카테고리 병합
            if (products.size() > 1) {
                sheet.addMergedRegion(new CellRangeAddress(categoryRowStart, currentRow - 1, 0, 0));
            }
        }

        return currentRow;
    }

    /**
     * 전체 합계 행 작성
     */
    private int writeGrandTotal(Sheet sheet, Map<String, CellStyle> styles,
            YearlyComparisonDTO.GrandTotal grandTotal,
            int currentRow) {
        Row totalRow = sheet.createRow(currentRow++);
        int colIndex = 0;

        // "합계" 표시 (3개 병합)
        createCell(totalRow, colIndex, "합계", styles.get("grandTotal"));
        sheet.addMergedRegion(new CellRangeAddress(currentRow - 1, currentRow - 1, colIndex, colIndex + 2));
        colIndex += 3;

        // 년도별 합계
        createNumericCell(totalRow, colIndex++, grandTotal.getYear1Amount(), styles.get("grandTotal"));
        createNumericCell(totalRow, colIndex++, grandTotal.getYear2Amount(), styles.get("grandTotal"));
        createNumericCell(totalRow, colIndex++, grandTotal.getYear3Amount(), styles.get("grandTotal"));

        // 증감률
        createGrowthRateCell(totalRow, colIndex++, grandTotal.getGrowthRate(), styles);

        return currentRow;
    }

    /**
     * 증감률 셀 생성
     */
    private void createGrowthRateCell(Row row, int column, BigDecimal growthRate, Map<String, CellStyle> styles) {
        Cell cell = row.createCell(column);

        if (growthRate == null) {
            cell.setCellValue("0.0%");
            cell.setCellStyle(styles.get("number"));
            return;
        }

        String value = String.format("%+.1f%%", growthRate.doubleValue());
        cell.setCellValue(value);

        if (growthRate.compareTo(BigDecimal.ZERO) > 0) {
            cell.setCellStyle(styles.get("growthPositive"));
        } else if (growthRate.compareTo(BigDecimal.ZERO) < 0) {
            cell.setCellStyle(styles.get("growthNegative"));
        } else {
            cell.setCellStyle(styles.get("number"));
        }
    }

    /**
     * 컬럼 너비 조정
     */
    private void adjustColumnWidths(Sheet sheet) {
        sheet.setColumnWidth(0, 3000); // 분류
        sheet.setColumnWidth(1, 3000); // 제품코드
        sheet.setColumnWidth(2, 6000); // 제품명
        sheet.setColumnWidth(3, 4000); // year1
        sheet.setColumnWidth(4, 4000); // year2
        sheet.setColumnWidth(5, 4000); // year3
        sheet.setColumnWidth(6, 3000); // 증감률
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