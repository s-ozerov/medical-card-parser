package ru.work.service.service.sheet;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.FontFamily;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


public abstract class SheetStyle {

    public static final String IF_CELL_IS_NULL = "-";

    public static void setLastCollWidth(final Sheet sheet, int widthPx) {
        sheet.setColumnWidth(sheet.getRow(sheet.getLastRowNum()).getLastCellNum() - 1, widthPx * 256);
    }

    public static void setLastCollWidthAuto(final Sheet sheet) {
        sheet.autoSizeColumn(sheet.getRow(sheet.getLastRowNum()).getLastCellNum() - 1);
    }

    public static CellStyle createHeaderTableCellsStyle(XSSFWorkbook workbook) {
        XSSFFont font = workbook.createFont();
        font.setFontHeightInPoints((short) 14);
        font.setFamily(FontFamily.MODERN);

        CellStyle cell = workbook.createCellStyle();
        generalSettings(cell, BorderStyle.MEDIUM);                      //BorderStyle.MEDIUM - граница ячейки
        cell.setFillForegroundColor(IndexedColors.SKY_BLUE.index);      //цвет заливки
        cell.setFillPattern(FillPatternType.DIAMONDS);                  //узор на фоне ячейки
        cell.setFont(font);
        cell.setWrapText(false);
        return cell;
    }

    public static CellStyle createStandardTableCellsStyle(XSSFWorkbook workbook) {
        XSSFFont font = workbook.createFont();
        font.setFontHeightInPoints((short) 12);
        font.setFamily(FontFamily.DECORATIVE);

        CellStyle style = workbook.createCellStyle();
        generalSettings(style, BorderStyle.THIN);
        style.setWrapText(true);
        return style;
    }

    public static CellStyle createHeaderSheetStyle(XSSFWorkbook workbook) {
        XSSFFont font = workbook.createFont();
        font.setFontHeightInPoints((short) 14);

        CellStyle style = workbook.createCellStyle();
        generalSettings(style, BorderStyle.NONE);
        style.setFont(font);

        return style;
    }

    public static CellStyle commonStyle(XSSFWorkbook workbook, int fontHeight, boolean bold) {
        XSSFFont font = workbook.createFont();
        font.setFontHeightInPoints((short) fontHeight);
        font.setBold(bold);

        CellStyle style = workbook.createCellStyle();
        generalSettings(style, BorderStyle.NONE);
        style.setFont(font);

        return style;
    }

    private static void generalSettings(CellStyle style,
                                        BorderStyle borderStyle) {
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderLeft(borderStyle);
        style.setBorderRight(borderStyle);
        style.setBorderBottom(borderStyle);
        style.setBorderTop(borderStyle);
    }
}
