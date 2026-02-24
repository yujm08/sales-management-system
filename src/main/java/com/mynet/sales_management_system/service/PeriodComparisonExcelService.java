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
            int titleEndRow = createTitle(sheet, styles, productName, 0);

            // 테이블은 3행(인덱스 2)부터 시작
            int tableStartRow = 2;

            int startColumn = 0; // 가로 시작 컬럼

            for (int i = 0; i < periodsData.size(); i++) {

                PeriodComparisonDTO.PeriodData periodData = periodsData.get(i);

                writePeriodBlock(sheet, styles, periodData, tableStartRow, startColumn, i + 1);

                startColumn += 5; // 4컬럼 + 1칸 공백
            }

            // 컬럼 너비 조정
            adjustColumnWidths(sheet, periodsData.size());

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

    private void writePeriodBlock(
            Sheet sheet,
            Map<String, CellStyle> styles,
            PeriodComparisonDTO.PeriodData periodData,
            int startRow,
            int startCol,
            int periodNumber) {

        int rowIdx = startRow;

        // 기간 제목
        Row headerRow = sheet.getRow(rowIdx);
        if (headerRow == null)
            headerRow = sheet.createRow(rowIdx);
        headerRow.setHeightInPoints(25);

        String periodTitle = String.format("기간 %d: %s ~ %s",
                periodNumber,
                periodData.getStartDate().format(DATE_FORMATTER),
                periodData.getEndDate().format(DATE_FORMATTER));

        Cell headerCell = headerRow.createCell(startCol);
        headerCell.setCellValue(periodTitle);
        headerCell.setCellStyle(styles.get("periodHeader"));

        sheet.addMergedRegion(new CellRangeAddress(
                rowIdx, rowIdx,
                startCol, startCol + 3));

        rowIdx++;

        // 테이블 헤더
        Row tableHeaderRow = sheet.getRow(rowIdx);
        if (tableHeaderRow == null)
            tableHeaderRow = sheet.createRow(rowIdx);

        createCell(tableHeaderRow, startCol, "날짜", styles.get("tableHeader"));
        createCell(tableHeaderRow, startCol + 1, "요일", styles.get("tableHeader"));
        createCell(tableHeaderRow, startCol + 2, "수량", styles.get("tableHeader"));
        createCell(tableHeaderRow, startCol + 3, "금액", styles.get("tableHeader"));

        rowIdx++;

        // 일별 데이터
        for (PeriodComparisonDTO.DailyData dailyData : periodData.getDailyDetails()) {

            Row dataRow = sheet.getRow(rowIdx);
            if (dataRow == null)
                dataRow = sheet.createRow(rowIdx);

            createCell(dataRow, startCol,
                    dailyData.getDate().format(DATE_FORMATTER),
                    styles.get("data"));

            createCell(dataRow, startCol + 1,
                    getDayOfWeekKorean(dailyData.getDate().getDayOfWeek().getValue()),
                    styles.get("data"));

            createNumericCell(dataRow, startCol + 2,
                    dailyData.getQuantity(),
                    styles.get("number"));

            createNumericCell(dataRow, startCol + 3,
                    dailyData.getAmount(),
                    styles.get("number"));

            rowIdx++;
        }

        // 합계 행
        Row summaryRow = sheet.getRow(rowIdx);
        if (summaryRow == null)
            summaryRow = sheet.createRow(rowIdx);

        createCell(summaryRow, startCol, "합계", styles.get("summary"));
        createCell(summaryRow, startCol + 1, "", styles.get("summary"));
        createNumericCell(summaryRow, startCol + 2, periodData.getTotalQuantity(), styles.get("summary"));
        createNumericCell(summaryRow, startCol + 3, periodData.getTotalAmount(), styles.get("summary"));

        rowIdx++;
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
    private void adjustColumnWidths(Sheet sheet, int periodCount) {

        int blockWidth = 5;

        for (int i = 0; i < periodCount; i++) {

            int baseCol = i * blockWidth;

            sheet.setColumnWidth(baseCol, 4000);
            sheet.setColumnWidth(baseCol + 1, 2500);
            sheet.setColumnWidth(baseCol + 2, 3500);
            sheet.setColumnWidth(baseCol + 3, 4500);
        }
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
