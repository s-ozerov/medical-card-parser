package ru.work.service.service.sheet.template;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.work.service.dto.DownloadDto;
import ru.work.service.dto.FileDto;
import ru.work.service.dto.ProcessResponse;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

import static ru.work.service.service.sheet.SheetStyle.IF_CELL_IS_NULL;

public interface SheetTemplate<T extends FileDto> {

    ProcessResponse<T> read(List<FileDto> files);

    DownloadDto prepare(String downloadFilename, ByteArrayOutputStream xlsxContent);

    void buildRowHeaders(XSSFWorkbook workbook, Sheet sheet);

    void buildRowData(XSSFWorkbook workbook, Sheet sheet, T doc);

    default <O> void addCell(Row row, O value, CellStyle style) {
        int last = row.getLastCellNum();

        Cell cell;
        if (last == -1) {
            cell = row.createCell(0);
        } else {
            cell = row.createCell(row.getLastCellNum());
        }

        cell.setCellStyle(style);

        if (value == null) {
            cell.setCellValue(IF_CELL_IS_NULL);
        } else {
            if (value instanceof LocalDate) {
                cell.setCellValue(((LocalDate) value).toString());
            } else if (StringUtils.isNotBlank(value.toString()) && (value.toString().chars().allMatch(Character::isDigit)
                                                                    || value instanceof Integer || value instanceof Long
                                                                    || value instanceof Float || value instanceof Double)) {
                cell.setCellValue(Double.parseDouble(value.toString()));
            } else {
                cell.setCellValue(value.toString());
            }
        }
    }

}
