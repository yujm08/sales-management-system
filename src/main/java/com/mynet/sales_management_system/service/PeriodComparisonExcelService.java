package com.mynet.sales_management_system.service;

import com.mynet.sales_management_system.dto.PeriodComparisonDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class PeriodComparisonExcelService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 기간별 비교 엑셀 파일 생성
     */
    public byte[] generatePeriodComparisonExcel(
            List<PeriodComparisonDTO.PeriodData> periodsData,
            String productName) throws IOException {

        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("기간별비교");

            // 스타일 생성
            Map<String, CellStyle> styles = createStyles(workbook);

            // 제목 행
            int currentRow = 0;
            currentRow = createTitle(sheet, styles, productName, currentRow);

            // 각 기간별로 시트에 추가
            for (int i = 0; i < periodsData.size(); i++) {
                PeriodComparisonDTO.PeriodData periodData = periodsData.get(i);

                // 기간 제목
                currentRow = createPeriodHeader(sheet, styles, periodData, currentRow, i + 1);

                // 일별 상세 데이터
                currentRow = writePeriodData(sheet, styles, periodData, currentRow);

                // 기간 사이 여백
                currentRow += 2;
            }

            // 컬럼 너비 조정
            adjustColumnWidths(sheet);

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

        // 제목 스타일
        CellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        styles.put("title", titleStyle);

        // 기간 헤더 스타일 (어두운 청록색)
        CellStyle periodHeaderStyle = workbook.createCellStyle();
        periodHeaderStyle.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
        periodHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        periodHeaderStyle.setFont(headerFont);
        periodHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
        periodHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        periodHeaderStyle.setBorderTop(BorderStyle.THIN);
        periodHeaderStyle.setBorderBottom(BorderStyle.THIN);
        periodHeaderStyle.setBorderLeft(BorderStyle.THIN);
        periodHeaderStyle.setBorderRight(BorderStyle.THIN);
        styles.put("periodHeader", periodHeaderStyle);

        // 합계 행 스타일
        CellStyle summaryStyle = workbook.createCellStyle();
        summaryStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        summaryStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        summaryStyle.setFont(boldFont);
        summaryStyle.setAlignment(HorizontalAlignment.RIGHT);
        summaryStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        summaryStyle.setBorderTop(BorderStyle.MEDIUM);
        summaryStyle.setBorderBottom(BorderStyle.THIN);
        summaryStyle.setBorderLeft(BorderStyle.THIN);
        summaryStyle.setBorderRight(BorderStyle.THIN);
        summaryStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
        styles.put("summary", summaryStyle);

        // 테이블 헤더
        CellStyle tableHeaderStyle = workbook.createCellStyle();
        tableHeaderStyle.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
        tableHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        tableHeaderStyle.setFont(boldFont);
        tableHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
        tableHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        tableHeaderStyle.setBorderTop(BorderStyle.THIN);
        tableHeaderStyle.setBorderBottom(BorderStyle.THIN);
        tableHeaderStyle.setBorderLeft(BorderStyle.THIN);
        tableHeaderStyle.setBorderRight(BorderStyle.THIN);
        styles.put("tableHeader", tableHeaderStyle);

        // 데이터 스타일
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setAlignment(HorizontalAlignment.CENTER);
        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        styles.put("data", dataStyle);

        // 숫자 포맷
        CellStyle numberStyle = workbook.createCellStyle();
        numberStyle.cloneStyleFrom(dataStyle);
        numberStyle.setAlignment(HorizontalAlignment.RIGHT);
        numberStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
        styles.put("number", numberStyle);

        return styles;
    }

    /**
     * 제목 행 작성
     */
    private int createTitle(Sheet sheet, Map<String, CellStyle> styles, String productName, int startRow) {
        Row titleRow = sheet.createRow(startRow++);
        titleRow.setHeightInPoints(25);

        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("기간별 비교 - " + (productName != null ? productName : "전체 제품"));
        titleCell.setCellStyle(styles.get("title"));

        sheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 3));

        startRow++; // 여백
        return startRow;
    }

    /**
     * 기간 헤더 작성
     */
    private int createPeriodHeader(Sheet sheet, Map<String, CellStyle> styles,
            PeriodComparisonDTO.PeriodData periodData,
            int startRow, int periodNumber) {
        // 기간 제목 행
        Row headerRow = sheet.createRow(startRow++);
        headerRow.setHeightInPoints(25);

        String periodTitle = String.format("기간 %d: %s ~ %s",
                periodNumber,
                periodData.getStartDate().format(DATE_FORMATTER),
                periodData.getEndDate().format(DATE_FORMATTER));

        Cell headerCell = headerRow.createCell(0);
        headerCell.setCellValue(periodTitle);
        headerCell.setCellStyle(styles.get("periodHeader"));

        sheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 3));

        // 합계 행
        Row summaryRow = sheet.createRow(startRow++);
        summaryRow.setHeightInPoints(22);

        createCell(summaryRow, 0, "합계", styles.get("summary"));
        createCell(summaryRow, 1, "", styles.get("summary"));
        createNumericCell(summaryRow, 2, periodData.getTotalQuantity(), styles.get("summary"));
        createNumericCell(summaryRow, 3, periodData.getTotalAmount(), styles.get("summary"));

        return startRow;
    }

    /**
     * 기간별 데이터 작성
     */
    private int writePeriodData(Sheet sheet, Map<String, CellStyle> styles,
            PeriodComparisonDTO.PeriodData periodData,
            int startRow) {
        // 테이블 헤더
        Row tableHeaderRow = sheet.createRow(startRow++);
        tableHeaderRow.setHeightInPoints(20);

        createCell(tableHeaderRow, 0, "날짜", styles.get("tableHeader"));
        createCell(tableHeaderRow, 1, "요일", styles.get("tableHeader"));
        createCell(tableHeaderRow, 2, "수량", styles.get("tableHeader"));
        createCell(tableHeaderRow, 3, "금액", styles.get("tableHeader"));

        // 일별 데이터
        for (PeriodComparisonDTO.DailyData dailyData : periodData.getDailyDetails()) {
            Row dataRow = sheet.createRow(startRow++);

            // 날짜
            createCell(dataRow, 0, dailyData.getDate().format(DATE_FORMATTER), styles.get("data"));

            // 요일
            String dayOfWeek = getDayOfWeekKorean(dailyData.getDate().getDayOfWeek().getValue());
            createCell(dataRow, 1, dayOfWeek, styles.get("data"));

            // 수량
            createNumericCell(dataRow, 2, dailyData.getQuantity(), styles.get("number"));

            // 금액
            createNumericCell(dataRow, 3, dailyData.getAmount(), styles.get("number"));
        }

        return startRow;
    }

    /**
     * 요일 한글 변환
     */
    private String getDayOfWeekKorean(int dayOfWeek) {
        String[] days = { "월", "화", "수", "목", "금", "토", "일" };
        return days[dayOfWeek - 1];
    }

    /**
     * 컬럼 너비 조정
     */
    private void adjustColumnWidths(Sheet sheet) {
        sheet.setColumnWidth(0, 4000); // 날짜
        sheet.setColumnWidth(1, 2500); // 요일
        sheet.setColumnWidth(2, 3500); // 수량
        sheet.setColumnWidth(3, 4500); // 금액
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