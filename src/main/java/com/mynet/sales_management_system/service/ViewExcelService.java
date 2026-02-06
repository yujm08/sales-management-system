package com.mynet.sales_management_system.service;

import com.mynet.sales_management_system.dto.ViewStatisticsDTO;
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
public class ViewExcelService {

    /**
     * 조회 페이지 엑셀 파일 생성
     */
    public byte[] generateViewExcel(
            Map<String, List<ViewStatisticsDTO>> statisticsData,
            Map<String, ViewStatisticsDTO> subtotalData,
            ViewStatisticsDTO grandTotalData,
            LocalDate targetDate,
            String companyName) throws IOException {

        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("조회");

            // 스타일 생성
            Map<String, CellStyle> styles = createStyles(workbook);

            // 헤더 작성
            int currentRow = 0;
            currentRow = createHeaders(sheet, styles, targetDate, currentRow);

            // 데이터 작성
            currentRow = writeData(sheet, styles, statisticsData, subtotalData, currentRow);

            // 합계 행 작성
            if (grandTotalData != null) {
                currentRow = writeGrandTotal(sheet, styles, grandTotalData, currentRow);
            }

            // 컬럼 너비 조정
            adjustColumnWidths(sheet);

            // 틀 고정 (A~E열, 1~2행)
            sheet.createFreezePane(5, 2);

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

        // 기본 데이터 스타일 (흰색)
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setAlignment(HorizontalAlignment.RIGHT);
        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        styles.put("data", dataStyle);

        // 카테고리 스타일
        CellStyle categoryStyle = workbook.createCellStyle();
        categoryStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        categoryStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        categoryStyle.setFont(boldFont);
        categoryStyle.setAlignment(HorizontalAlignment.CENTER);
        categoryStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        categoryStyle.setBorderTop(BorderStyle.THIN);
        categoryStyle.setBorderBottom(BorderStyle.THIN);
        categoryStyle.setBorderLeft(BorderStyle.THIN);
        categoryStyle.setBorderRight(BorderStyle.THIN);
        styles.put("category", categoryStyle);

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

        // 제품 데이터 행 스타일 (연한 녹색)
        CellStyle greenStyle = workbook.createCellStyle();
        greenStyle.cloneStyleFrom(numberStyle);
        greenStyle.setFillPattern(FillPatternType.NO_FILL);
        styles.put("green", greenStyle);

        // 제품명 스타일 (연한 녹색 + 좌측 정렬)
        CellStyle greenProductStyle = workbook.createCellStyle();
        greenProductStyle.cloneStyleFrom(greenStyle);
        greenProductStyle.setAlignment(HorizontalAlignment.LEFT);
        styles.put("greenProduct", greenProductStyle);

        // 이익 스타일 (빨간색 텍스트)
        Font redFont = workbook.createFont();
        redFont.setBold(true);
        redFont.setColor(IndexedColors.RED.getIndex());
        redFont.setFontHeightInPoints((short) 10);

        CellStyle profitStyle = workbook.createCellStyle();
        profitStyle.cloneStyleFrom(greenStyle);
        profitStyle.setFont(redFont);
        styles.put("profit", profitStyle);

        // 소계 스타일
        CellStyle subtotalStyle = workbook.createCellStyle();
        subtotalStyle.cloneStyleFrom(numberStyle);
        subtotalStyle.setFont(boldFont);
        subtotalStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        subtotalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put("subtotal", subtotalStyle);

        // 소계 이익 스타일 (회색 + 빨간 글씨)
        CellStyle subtotalProfitStyle = workbook.createCellStyle();
        subtotalProfitStyle.cloneStyleFrom(subtotalStyle);
        subtotalProfitStyle.setFont(redFont);
        styles.put("subtotalProfit", subtotalProfitStyle);

        // 합계 스타일
        CellStyle totalStyle = workbook.createCellStyle();
        totalStyle.cloneStyleFrom(numberStyle);
        totalStyle.setFont(whiteFont);
        totalStyle.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
        totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put("total", totalStyle);

        return styles;
    }

    /**
     * 헤더 작성
     */
    private int createHeaders(Sheet sheet, Map<String, CellStyle> styles, LocalDate targetDate, int startRow) {
        // 첫 번째 헤더 행
        Row headerRow1 = sheet.createRow(startRow++);
        headerRow1.setHeightInPoints(20);

        int colIndex = 0;

        // 제품 영역 (5개)
        createCell(headerRow1, colIndex, "제품", styles.get("header"));
        sheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, colIndex, colIndex + 4));
        colIndex += 5;

        // 일 합계 (3개)
        createCell(headerRow1, colIndex, "일 합계", styles.get("header"));
        sheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, colIndex, colIndex + 2));
        colIndex += 3;

        // 월 합계 (3개)
        createCell(headerRow1, colIndex, "월 합계", styles.get("header"));
        sheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, colIndex, colIndex + 2));
        colIndex += 3;

        // 목표수량 (2개)
        createCell(headerRow1, colIndex, "목표수량", styles.get("header"));
        sheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, colIndex, colIndex + 1));
        colIndex += 2;

        // 두 번째 헤더 행
        Row headerRow2 = sheet.createRow(startRow++);
        headerRow2.setHeightInPoints(20);

        colIndex = 0;

        // 제품 상세 헤더
        createCell(headerRow2, colIndex++, "분류", styles.get("header"));
        createCell(headerRow2, colIndex++, "제품코드", styles.get("header"));
        createCell(headerRow2, colIndex++, "제품명", styles.get("header"));
        createCell(headerRow2, colIndex++, "원가", styles.get("header"));
        createCell(headerRow2, colIndex++, "공급가", styles.get("header"));

        // 일 합계 상세
        createCell(headerRow2, colIndex++, "수량", styles.get("header"));
        createCell(headerRow2, colIndex++, "금액", styles.get("header"));
        createCell(headerRow2, colIndex++, "이익", styles.get("header"));

        // 월 합계 상세
        createCell(headerRow2, colIndex++, "수량", styles.get("header"));
        createCell(headerRow2, colIndex++, "금액", styles.get("header"));
        createCell(headerRow2, colIndex++, "이익", styles.get("header"));

        // 목표수량 상세
        String monthStr = targetDate.format(DateTimeFormatter.ofPattern("M"));
        createCell(headerRow2, colIndex++, monthStr + "월", styles.get("header"));
        createCell(headerRow2, colIndex++, "달성률", styles.get("header"));

        return startRow;
    }

    /**
     * 데이터 작성
     */
    private int writeData(Sheet sheet, Map<String, CellStyle> styles,
            Map<String, List<ViewStatisticsDTO>> statisticsData,
            Map<String, ViewStatisticsDTO> subtotalData,
            int startRow) {

        int currentRow = startRow;
        int categoryRowStart;

        // 카테고리별 순회
        for (Map.Entry<String, List<ViewStatisticsDTO>> categoryEntry : statisticsData.entrySet()) {
            String category = categoryEntry.getKey();
            List<ViewStatisticsDTO> products = categoryEntry.getValue();

            categoryRowStart = currentRow;

            // 제품별 데이터 작성
            for (ViewStatisticsDTO product : products) {
                Row dataRow = sheet.createRow(currentRow++);
                int colIndex = 0;

                // 카테고리 (첫 행에만)
                if (currentRow - 1 == categoryRowStart) {
                    createCell(dataRow, colIndex, category, styles.get("category"));
                }
                colIndex++;

                // 제품코드 (녹색)
                createCell(dataRow, colIndex++, product.getProductCode(), styles.get("green"));

                // 제품명 (녹색 + 좌측 정렬)
                createCell(dataRow, colIndex++, product.getProductName(), styles.get("greenProduct"));

                // 원가 (녹색)
                createNumericCell(dataRow, colIndex++, product.getCostPrice(), styles.get("green"));

                // 공급가 (녹색)
                createNumericCell(dataRow, colIndex++, product.getSupplyPrice(), styles.get("green"));

                // 일 합계
                if (product.getDailySummary() != null) {
                    createNumericCell(dataRow, colIndex++, product.getDailySummary().getQuantity(),
                            styles.get("green"));
                    createNumericCell(dataRow, colIndex++, product.getDailySummary().getAmount(), styles.get("green"));
                    createNumericCell(dataRow, colIndex++, product.getDailySummary().getProfit(), styles.get("profit"));
                } else {
                    createCell(dataRow, colIndex++, "-", styles.get("green"));
                    createCell(dataRow, colIndex++, "-", styles.get("green"));
                    createCell(dataRow, colIndex++, "-", styles.get("profit"));
                }

                // 월 합계
                if (product.getMonthlySummary() != null) {
                    createNumericCell(dataRow, colIndex++, product.getMonthlySummary().getQuantity(),
                            styles.get("green"));
                    createNumericCell(dataRow, colIndex++, product.getMonthlySummary().getAmount(),
                            styles.get("green"));
                    createNumericCell(dataRow, colIndex++, product.getMonthlySummary().getProfit(),
                            styles.get("profit"));
                } else {
                    createCell(dataRow, colIndex++, "-", styles.get("green"));
                    createCell(dataRow, colIndex++, "-", styles.get("green"));
                    createCell(dataRow, colIndex++, "-", styles.get("profit"));
                }

                // 목표수량
                if (product.getTargetData() != null) {
                    createNumericCell(dataRow, colIndex++, product.getTargetData().getTargetQuantity(),
                            styles.get("green"));

                    // 달성률
                    Cell rateCell = dataRow.createCell(colIndex++);
                    if (product.getTargetData().getAchievementRate() != null) {
                        rateCell.setCellValue(product.getTargetData().getAchievementRate().doubleValue() + "%");
                    } else {
                        rateCell.setCellValue("0%");
                    }
                    rateCell.setCellStyle(styles.get("green"));
                } else {
                    createCell(dataRow, colIndex++, "-", styles.get("green"));
                    createCell(dataRow, colIndex++, "0%", styles.get("green"));
                }
            }

            // 카테고리 병합
            if (products.size() > 1) {
                sheet.addMergedRegion(new CellRangeAddress(categoryRowStart, currentRow - 1, 0, 0));
            }

            // 소계 행 작성
            ViewStatisticsDTO subtotal = subtotalData.get(category);
            if (subtotal != null) {
                currentRow = writeSubtotal(sheet, styles, subtotal, currentRow);
            }
        }

        return currentRow;
    }

    /**
     * 소계 행 작성
     */
    private int writeSubtotal(Sheet sheet, Map<String, CellStyle> styles,
            ViewStatisticsDTO subtotal, int currentRow) {
        Row subtotalRow = sheet.createRow(currentRow++);
        int colIndex = 0;

        // "소계" 표시 (5개 컬럼 병합)
        createCell(subtotalRow, colIndex, "소계", styles.get("subtotal"));
        sheet.addMergedRegion(new CellRangeAddress(currentRow - 1, currentRow - 1, colIndex, colIndex + 4));
        colIndex += 5;

        // 일 합계
        if (subtotal.getDailySummary() != null) {
            createNumericCell(subtotalRow, colIndex++, subtotal.getDailySummary().getQuantity(),
                    styles.get("subtotal"));
            createNumericCell(subtotalRow, colIndex++, subtotal.getDailySummary().getAmount(), styles.get("subtotal"));
            createNumericCell(subtotalRow, colIndex++, subtotal.getDailySummary().getProfit(),
                    styles.get("subtotalProfit"));
        } else {
            createCell(subtotalRow, colIndex++, "-", styles.get("subtotal"));
            createCell(subtotalRow, colIndex++, "-", styles.get("subtotal"));
            createCell(subtotalRow, colIndex++, "-", styles.get("subtotalProfit"));
        }

        // 월 합계
        if (subtotal.getMonthlySummary() != null) {
            createNumericCell(subtotalRow, colIndex++, subtotal.getMonthlySummary().getQuantity(),
                    styles.get("subtotal"));
            createNumericCell(subtotalRow, colIndex++, subtotal.getMonthlySummary().getAmount(),
                    styles.get("subtotal"));
            createNumericCell(subtotalRow, colIndex++, subtotal.getMonthlySummary().getProfit(),
                    styles.get("subtotalProfit"));
        } else {
            createCell(subtotalRow, colIndex++, "-", styles.get("subtotal"));
            createCell(subtotalRow, colIndex++, "-", styles.get("subtotal"));
            createCell(subtotalRow, colIndex++, "-", styles.get("subtotalProfit"));
        }

        // 목표수량
        if (subtotal.getTargetData() != null) {
            createNumericCell(subtotalRow, colIndex++, subtotal.getTargetData().getTargetQuantity(),
                    styles.get("subtotal"));

            Cell rateCell = subtotalRow.createCell(colIndex++);
            if (subtotal.getTargetData().getAchievementRate() != null) {
                rateCell.setCellValue(subtotal.getTargetData().getAchievementRate().doubleValue() + "%");
            } else {
                rateCell.setCellValue("0%");
            }
            rateCell.setCellStyle(styles.get("subtotal"));
        } else {
            createCell(subtotalRow, colIndex++, "-", styles.get("subtotal"));
            createCell(subtotalRow, colIndex++, "0%", styles.get("subtotal"));
        }

        return currentRow;
    }

    /**
     * 전체 합계 행 작성
     */
    private int writeGrandTotal(Sheet sheet, Map<String, CellStyle> styles,
            ViewStatisticsDTO grandTotal, int currentRow) {
        Row totalRow = sheet.createRow(currentRow++);
        int colIndex = 0;

        // "전체 합계" 표시 (5개 컬럼 병합)
        createCell(totalRow, colIndex, "전체 합계", styles.get("total"));
        sheet.addMergedRegion(new CellRangeAddress(currentRow - 1, currentRow - 1, colIndex, colIndex + 4));
        colIndex += 5;

        // 일 합계
        if (grandTotal.getDailySummary() != null) {
            createNumericCell(totalRow, colIndex++, grandTotal.getDailySummary().getQuantity(), styles.get("total"));
            createNumericCell(totalRow, colIndex++, grandTotal.getDailySummary().getAmount(), styles.get("total"));
            createNumericCell(totalRow, colIndex++, grandTotal.getDailySummary().getProfit(), styles.get("total"));
        } else {
            createCell(totalRow, colIndex++, "-", styles.get("total"));
            createCell(totalRow, colIndex++, "-", styles.get("total"));
            createCell(totalRow, colIndex++, "-", styles.get("total"));
        }

        // 월 합계
        if (grandTotal.getMonthlySummary() != null) {
            createNumericCell(totalRow, colIndex++, grandTotal.getMonthlySummary().getQuantity(), styles.get("total"));
            createNumericCell(totalRow, colIndex++, grandTotal.getMonthlySummary().getAmount(), styles.get("total"));
            createNumericCell(totalRow, colIndex++, grandTotal.getMonthlySummary().getProfit(), styles.get("total"));
        } else {
            createCell(totalRow, colIndex++, "-", styles.get("total"));
            createCell(totalRow, colIndex++, "-", styles.get("total"));
            createCell(totalRow, colIndex++, "-", styles.get("total"));
        }

        // 목표수량
        if (grandTotal.getTargetData() != null) {
            createNumericCell(totalRow, colIndex++, grandTotal.getTargetData().getTargetQuantity(),
                    styles.get("total"));

            Cell rateCell = totalRow.createCell(colIndex++);
            if (grandTotal.getTargetData().getAchievementRate() != null) {
                rateCell.setCellValue(grandTotal.getTargetData().getAchievementRate().doubleValue() + "%");
            } else {
                rateCell.setCellValue("0%");
            }
            rateCell.setCellStyle(styles.get("total"));
        } else {
            createCell(totalRow, colIndex++, "-", styles.get("total"));
            createCell(totalRow, colIndex++, "0%", styles.get("total"));
        }

        return currentRow;
    }

    /**
     * 컬럼 너비 조정
     */
    private void adjustColumnWidths(Sheet sheet) {
        sheet.setColumnWidth(0, 3000); // 분류
        sheet.setColumnWidth(1, 3000); // 제품코드
        sheet.setColumnWidth(2, 6000); // 제품명
        sheet.setColumnWidth(3, 3500); // 원가
        sheet.setColumnWidth(4, 3500); // 공급가
        sheet.setColumnWidth(5, 3000); // 일 수량
        sheet.setColumnWidth(6, 4000); // 일 금액
        sheet.setColumnWidth(7, 4000); // 일 이익
        sheet.setColumnWidth(8, 3000); // 월 수량
        sheet.setColumnWidth(9, 4000); // 월 금액
        sheet.setColumnWidth(10, 4000); // 월 이익
        sheet.setColumnWidth(11, 3000); // 목표
        sheet.setColumnWidth(12, 3000); // 달성률
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