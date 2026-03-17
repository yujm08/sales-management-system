package com.mynet.sales_management_system.service;

import com.mynet.sales_management_system.dto.PeriodComparisonDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PeriodComparisonExcelService {

    // 카테고리 배경색 (초록 계열 4단계)
    private static final byte[][] CATEGORY_COLORS = {
            { (byte) 0xE8, (byte) 0xF5, (byte) 0xE8 },
            { (byte) 0xD4, (byte) 0xF4, (byte) 0xD4 },
            { (byte) 0xC0, (byte) 0xF0, (byte) 0xC0 },
            { (byte) 0xB3, (byte) 0xE6, (byte) 0xB3 }
    };

    // 금액 배경색 (주황 계열 4단계)
    private static final byte[][] AMOUNT_COLORS = {
            { (byte) 0xFF, (byte) 0xF2, (byte) 0xEB },
            { (byte) 0xFF, (byte) 0xE8, (byte) 0xE0 },
            { (byte) 0xFF, (byte) 0xE0, (byte) 0xD6 },
            { (byte) 0xFF, (byte) 0xD6, (byte) 0xCC }
    };

    public byte[] generatePeriodComparisonExcel(
            PeriodComparisonDTO.ComparisonResult data) throws IOException {

        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("기간별비교");

            int periodCount = data.getPeriodLabels().size();

            // 스타일 맵 생성
            Map<String, CellStyle> styles = createStyles(workbook, periodCount);

            // ── 헤더 1행: 제품(colspan 3) + 기간라벨(colspan 2씩)
            Row row1 = sheet.createRow(0);
            row1.setHeightInPoints(28);

            Cell thProduct = row1.createCell(0);
            thProduct.setCellValue("제품");
            thProduct.setCellStyle(styles.get("header1"));
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));

            for (int i = 0; i < periodCount; i++) {
                int col = 3 + i * 2;
                Cell thPeriod = row1.createCell(col);
                thPeriod.setCellValue(data.getPeriodLabels().get(i));
                thPeriod.setCellStyle(styles.get("header1"));
                // colspan 2 병합
                sheet.addMergedRegion(new CellRangeAddress(0, 0, col, col + 1));
                // 빈 셀에도 스타일 적용 (병합된 우측 셀)
                row1.createCell(col + 1).setCellStyle(styles.get("header1"));
            }

            // ── 헤더 2행: 분류/제품코드/제품명 + (수량/금액) × N
            Row row2 = sheet.createRow(1);
            row2.setHeightInPoints(22);

            String[] baseHeaders = { "분류", "제품코드", "제품명" };
            for (int i = 0; i < baseHeaders.length; i++) {
                Cell c = row2.createCell(i);
                c.setCellValue(baseHeaders[i]);
                c.setCellStyle(styles.get("header2"));
            }
            for (int i = 0; i < periodCount; i++) {
                int col = 3 + i * 2;
                Cell cQty = row2.createCell(col);
                cQty.setCellValue("수량");
                cQty.setCellStyle(styles.get("header2"));

                Cell cAmt = row2.createCell(col + 1);
                cAmt.setCellValue("금액");
                cAmt.setCellStyle(styles.get("header2"));
            }

            // ── 데이터 행
            int rowIdx = 2;
            List<PeriodComparisonDTO.CategoryData> categories = data.getCategories();

            for (int catIdx = 0; catIdx < categories.size(); catIdx++) {
                PeriodComparisonDTO.CategoryData cat = categories.get(catIdx);
                List<PeriodComparisonDTO.ProductPeriodData> products = cat.getProducts();

                CellStyle catCellStyle = createColorStyle(workbook, CATEGORY_COLORS[catIdx % 4], false, false);
                CellStyle catNameStyle = createColorStyle(workbook, CATEGORY_COLORS[catIdx % 4], false, true);
                CellStyle amtStyle = createColorStyle(workbook, AMOUNT_COLORS[catIdx % 4], true, false);
                CellStyle categoryCellSt = createCategoryCellStyle(workbook);

                for (int pIdx = 0; pIdx < products.size(); pIdx++) {
                    PeriodComparisonDTO.ProductPeriodData product = products.get(pIdx);
                    Row row = sheet.createRow(rowIdx);
                    row.setHeightInPoints(20);

                    // 분류 (첫 행에만, rowspan 대신 병합)
                    if (pIdx == 0) {
                        Cell catCell = row.createCell(0);
                        catCell.setCellValue(cat.getCategory());
                        catCell.setCellStyle(categoryCellSt);
                        if (products.size() > 1) {
                            sheet.addMergedRegion(new CellRangeAddress(
                                    rowIdx, rowIdx + products.size() - 1, 0, 0));
                        }
                    }

                    // 제품코드
                    Cell codeCell = row.createCell(1);
                    codeCell.setCellValue(product.getProductCode());
                    codeCell.setCellStyle(catCellStyle);

                    // 제품명
                    Cell nameCell = row.createCell(2);
                    nameCell.setCellValue(product.getProductName());
                    nameCell.setCellStyle(catNameStyle);

                    // 기간별 수량 + 금액
                    List<PeriodComparisonDTO.PeriodAmount> amounts = product.getPeriodAmounts();
                    for (int i = 0; i < amounts.size(); i++) {
                        int col = 3 + i * 2;
                        PeriodComparisonDTO.PeriodAmount pa = amounts.get(i);

                        // 수량 (카테고리 배경)
                        Cell qtyCell = row.createCell(col);
                        if (pa.getQuantity() != null && pa.getQuantity() != 0) {
                            qtyCell.setCellValue(pa.getQuantity());
                        } else {
                            qtyCell.setCellValue("-");
                        }
                        qtyCell.setCellStyle(catCellStyle);

                        // 금액 (주황 배경)
                        Cell amtCell = row.createCell(col + 1);
                        if (pa.getAmount() != null && pa.getAmount().longValue() != 0) {
                            amtCell.setCellValue(pa.getAmount().longValue());
                        } else {
                            amtCell.setCellValue("-");
                        }
                        amtCell.setCellStyle(amtStyle);
                    }

                    rowIdx++;
                }
            }

            // ── 합계 행
            Row totalRow = sheet.createRow(rowIdx);
            totalRow.setHeightInPoints(22);

            CellStyle totalLabelStyle = createTotalLabelStyle(workbook);
            CellStyle totalNumStyle = createTotalNumStyle(workbook);

            Cell totalLabel = totalRow.createCell(0);
            totalLabel.setCellValue("합계");
            totalLabel.setCellStyle(totalLabelStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 2));
            totalRow.createCell(1).setCellStyle(totalLabelStyle);
            totalRow.createCell(2).setCellStyle(totalLabelStyle);

            List<PeriodComparisonDTO.PeriodAmount> grand = data.getGrandTotal();
            for (int i = 0; i < grand.size(); i++) {
                int col = 3 + i * 2;
                PeriodComparisonDTO.PeriodAmount pa = grand.get(i);

                Cell qtyCell = totalRow.createCell(col);
                if (pa.getQuantity() != null && pa.getQuantity() != 0) {
                    qtyCell.setCellValue(pa.getQuantity());
                } else {
                    qtyCell.setCellValue("-");
                }
                qtyCell.setCellStyle(totalNumStyle);

                Cell amtCell = totalRow.createCell(col + 1);
                if (pa.getAmount() != null && pa.getAmount().longValue() != 0) {
                    amtCell.setCellValue(pa.getAmount().longValue());
                } else {
                    amtCell.setCellValue("-");
                }
                amtCell.setCellStyle(totalNumStyle);
            }

            // ── 컬럼 너비
            sheet.setColumnWidth(0, 3500); // 분류
            sheet.setColumnWidth(1, 3000); // 제품코드
            sheet.setColumnWidth(2, 7000); // 제품명
            for (int i = 0; i < periodCount; i++) {
                sheet.setColumnWidth(3 + i * 2, 3500); // 수량
                sheet.setColumnWidth(3 + i * 2 + 1, 5000); // 금액
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    // ── 스타일 헬퍼들 ──────────────────────────────────────────

    private Map<String, CellStyle> createStyles(XSSFWorkbook workbook, int periodCount) {
        Map<String, CellStyle> styles = new HashMap<>();

        Font whiteFont = workbook.createFont();
        whiteFont.setBold(true);
        whiteFont.setColor(IndexedColors.WHITE.getIndex());
        whiteFont.setFontHeightInPoints((short) 11);

        Font blackBoldFont = workbook.createFont();
        blackBoldFont.setBold(true);
        blackBoldFont.setFontHeightInPoints((short) 10);

        // 헤더 1행 스타일 (#24595e)
        XSSFCellStyle header1 = workbook.createCellStyle();
        header1.setFillForegroundColor(new XSSFColor(new byte[] { (byte) 0x24, (byte) 0x59, (byte) 0x5e }, null));
        header1.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        header1.setFont(whiteFont);
        header1.setAlignment(HorizontalAlignment.CENTER);
        header1.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(header1);
        styles.put("header1", header1);

        // 헤더 2행 스타일 (#d3d3d3)
        XSSFCellStyle header2 = workbook.createCellStyle();
        header2.setFillForegroundColor(new XSSFColor(new byte[] { (byte) 0xD3, (byte) 0xD3, (byte) 0xD3 }, null));
        header2.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        header2.setFont(blackBoldFont);
        header2.setAlignment(HorizontalAlignment.CENTER);
        header2.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(header2);
        styles.put("header2", header2);

        return styles;
    }

    private XSSFCellStyle createColorStyle(XSSFWorkbook workbook, byte[] rgb,
            boolean isAmount, boolean isName) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(rgb, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(isName ? HorizontalAlignment.LEFT : HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        if (isAmount) {
            style.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
        }
        setBorder(style);
        return style;
    }

    private XSSFCellStyle createCategoryCellStyle(XSSFWorkbook workbook) {
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(new byte[] { (byte) 0xF1, (byte) 0xF3, (byte) 0xF5 }, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFont(boldFont);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(style);
        return style;
    }

    private XSSFCellStyle createTotalLabelStyle(XSSFWorkbook workbook) {
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(new byte[] { (byte) 0xDE, (byte) 0xE2, (byte) 0xE6 }, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFont(boldFont);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(style);
        return style;
    }

    private XSSFCellStyle createTotalNumStyle(XSSFWorkbook workbook) {
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(new byte[] { (byte) 0xDE, (byte) 0xE2, (byte) 0xE6 }, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFont(boldFont);
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
        setBorder(style);
        return style;
    }

    private void setBorder(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}