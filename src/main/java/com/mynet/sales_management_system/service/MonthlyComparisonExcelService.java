package com.mynet.sales_management_system.service;

import com.mynet.sales_management_system.dto.MonthlyComparisonDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class MonthlyComparisonExcelService {

    /**
     * 월별 비교 엑셀 파일 생성
     */
    public byte[] generateMonthlyComparisonExcel(
            List<MonthlyComparisonDTO.CategoryData> monthlyData,
            MonthlyComparisonDTO.GrandTotal grandTotal,
            int year,
            String companyName) throws IOException {

        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("월별비교");

            // 스타일 생성
            Map<String, CellStyle> styles = createStyles(workbook);

            // 헤더 작성
            int currentRow = 0;
            currentRow = createHeaders(sheet, styles, currentRow);

            // 데이터 작성
            currentRow = writeData(sheet, styles, monthlyData, currentRow);

            // 합계 행 작성
            if (grandTotal != null) {
                currentRow = writeGrandTotal(sheet, styles, grandTotal, currentRow);
            }

            // 컬럼 너비 조정
            adjustColumnWidths(sheet);

            // 틀 고정 (A~C열, 1~2행)
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

        // 데이터 셀 스타일 (기본)
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setAlignment(HorizontalAlignment.RIGHT);
        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        styles.put("data", dataStyle);

        // 제품 정보 스타일 (좌측 정렬)
        CellStyle productStyle = workbook.createCellStyle();
        productStyle.cloneStyleFrom(dataStyle);
        productStyle.setAlignment(HorizontalAlignment.LEFT);
        styles.put("product", productStyle);

        // 숫자 포맷 스타일
        CellStyle numberStyle = workbook.createCellStyle();
        numberStyle.cloneStyleFrom(dataStyle);
        numberStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
        styles.put("number", numberStyle);

        // 카테고리별 배경색 (4가지)
        String[] colorNames = { "green1", "green2", "green3", "green4" };
        IndexedColors[] colors = {
                IndexedColors.LIGHT_GREEN,
                IndexedColors.LIGHT_GREEN,
                IndexedColors.LIGHT_GREEN,
                IndexedColors.LIGHT_GREEN
        };

        for (int i = 0; i < 4; i++) {
            CellStyle colorStyle = workbook.createCellStyle();
            colorStyle.cloneStyleFrom(numberStyle);
            colorStyle.setFillForegroundColor(colors[i].getIndex());
            colorStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            styles.put(colorNames[i], colorStyle);
        }

        // 합계 컬럼 스타일
        CellStyle totalQtyStyle = workbook.createCellStyle();
        totalQtyStyle.cloneStyleFrom(numberStyle);
        totalQtyStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        totalQtyStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put("totalQty", totalQtyStyle);

        CellStyle totalAmtStyle = workbook.createCellStyle();
        totalAmtStyle.cloneStyleFrom(numberStyle);
        totalAmtStyle.setFillForegroundColor(IndexedColors.TAN.getIndex());
        totalAmtStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put("totalAmt", totalAmtStyle);

        // 전체 합계 행 스타일
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
    private int createHeaders(Sheet sheet, Map<String, CellStyle> styles, int startRow) {
        // 첫 번째 헤더 행
        Row headerRow1 = sheet.createRow(startRow++);
        headerRow1.setHeightInPoints(20);

        int colIndex = 0;

        // 구분 (3개 컬럼 병합)
        createCell(headerRow1, colIndex, "구분", styles.get("header"));
        sheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, colIndex, colIndex + 2));
        colIndex += 3;

        // 1월 ~ 12월 (각 2개씩 병합)
        for (int month = 1; month <= 12; month++) {
            createCell(headerRow1, colIndex, month + "월", styles.get("header"));
            sheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, colIndex, colIndex + 1));
            colIndex += 2;
        }

        // 합계 (2개 병합)
        createCell(headerRow1, colIndex, "합계", styles.get("header"));
        sheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, colIndex, colIndex + 1));

        // 두 번째 헤더 행
        Row headerRow2 = sheet.createRow(startRow++);
        headerRow2.setHeightInPoints(20);

        colIndex = 0;

        // 구분 상세
        createCell(headerRow2, colIndex++, "분류", styles.get("subHeader"));
        createCell(headerRow2, colIndex++, "제품코드", styles.get("subHeader"));
        createCell(headerRow2, colIndex++, "제품명", styles.get("subHeader"));

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
            List<MonthlyComparisonDTO.CategoryData> monthlyData,
            int startRow) {

        int currentRow = startRow;
        int categoryColorIndex = 0;

        for (MonthlyComparisonDTO.CategoryData categoryData : monthlyData) {
            String colorStyleName = "green" + ((categoryColorIndex % 4) + 1);
            categoryColorIndex++;

            List<MonthlyComparisonDTO.ProductMonthlyData> products = categoryData.getProducts();
            int categoryRowStart = currentRow;

            for (MonthlyComparisonDTO.ProductMonthlyData product : products) {
                Row dataRow = sheet.createRow(currentRow++);
                int colIndex = 0;

                // 카테고리 (첫 행에만, 병합)
                if (currentRow - 1 == categoryRowStart) {
                    createCell(dataRow, colIndex, categoryData.getCategory(), styles.get("category"));
                }
                colIndex++;

                // 제품코드
                createCell(dataRow, colIndex++, product.getProductCode(), styles.get("data"));

                // 제품명
                createCell(dataRow, colIndex++, product.getProductName(), styles.get("product"));

                // 1월 ~ 12월 데이터
                for (MonthlyComparisonDTO.MonthData monthData : product.getMonthlyData()) {
                    createNumericCell(dataRow, colIndex++, monthData.getQuantity(), styles.get(colorStyleName));
                    createNumericCell(dataRow, colIndex++, monthData.getAmount(), styles.get(colorStyleName));
                }

                // 합계
                createNumericCell(dataRow, colIndex++, product.getTotalQuantity(), styles.get("totalQty"));
                createNumericCell(dataRow, colIndex++, product.getTotalAmount(), styles.get("totalAmt"));
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
            MonthlyComparisonDTO.GrandTotal grandTotal,
            int currentRow) {
        Row totalRow = sheet.createRow(currentRow++);
        int colIndex = 0;

        // "전체 합계" 표시 (3개 컬럼 병합)
        createCell(totalRow, colIndex, "전체 합계", styles.get("grandTotal"));
        sheet.addMergedRegion(new CellRangeAddress(currentRow - 1, currentRow - 1, colIndex, colIndex + 2));
        colIndex += 3;

        // 1월 ~ 12월 합계
        for (MonthlyComparisonDTO.MonthData monthData : grandTotal.getMonthlyTotals()) {
            createNumericCell(totalRow, colIndex++, monthData.getQuantity(), styles.get("grandTotal"));
            createNumericCell(totalRow, colIndex++, monthData.getAmount(), styles.get("grandTotal"));
        }

        // 전체 합계
        createNumericCell(totalRow, colIndex++, grandTotal.getTotalQuantity(), styles.get("grandTotal"));
        createNumericCell(totalRow, colIndex++, grandTotal.getTotalAmount(), styles.get("grandTotal"));

        return currentRow;
    }

    /**
     * 컬럼 너비 조정
     */
    private void adjustColumnWidths(Sheet sheet) {
        sheet.setColumnWidth(0, 3000); // 분류
        sheet.setColumnWidth(1, 3000); // 제품코드
        sheet.setColumnWidth(2, 6000); // 제품명

        // 1~12월 + 합계 (각 2개씩)
        for (int i = 3; i < 29; i++) {
            sheet.setColumnWidth(i, 3500);
        }
    }

    /**
     * 셀 생성 (문자열)
     */
    private Cell createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
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