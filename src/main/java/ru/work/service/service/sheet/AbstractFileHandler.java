package ru.work.service.service.sheet;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.work.service.dto.DownloadDto;
import ru.work.service.dto.FileDto;
import ru.work.service.dto.ProcessResponse;
import ru.work.service.helper.FileHelper;
import ru.work.service.service.sheet.template.SheetTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public abstract class AbstractFileHandler<T extends FileDto> {

    private final SheetTemplate<T> sheetTemplate;

    public AbstractFileHandler(SheetTemplate<T> template) {
        this.sheetTemplate = template;
    }

    public ProcessResponse<T> readFile(FileDto file) {
        return sheetTemplate.read(Collections.singletonList(file));
    }

    public ProcessResponse<T> readFile(String path) {
        List<FileDto> files = FileHelper.getFilesDOCorDOCXByPatch(path);
        return sheetTemplate.read(files);
    }

    protected DownloadDto convert(String downloadFilename, XSSFWorkbook workbook, Sheet sheet, ProcessResponse<T> files) throws IOException {
        sheetTemplate.buildRowHeaders(workbook, sheet);

        for (T fileTyped : files.getProcessedFiles()) {
            sheetTemplate.buildRowData(workbook, sheet, fileTyped);
        }

        try (ByteArrayOutputStream xlsxContent = new ByteArrayOutputStream()) {
            workbook.write(xlsxContent);
            return sheetTemplate.prepare(downloadFilename, xlsxContent);
        }
    }
}
