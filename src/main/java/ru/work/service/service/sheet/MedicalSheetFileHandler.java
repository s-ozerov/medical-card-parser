package ru.work.service.service.sheet;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import ru.work.service.dto.DownloadDto;
import ru.work.service.dto.ProcessResponse;
import ru.work.service.dto.medical.MedicalDocFile;
import ru.work.service.service.sheet.template.MedicalTemplate;

/**
 * Сервис для работы с файлами расширения .xls и .xlsx
 */
@Slf4j
@Service
public class MedicalSheetFileHandler extends AbstractFileHandler<MedicalDocFile> {

    @Autowired
    public MedicalSheetFileHandler(MedicalTemplate medicalTemplate) {
        super(medicalTemplate);
    }

    public DownloadDto convertDOCToXLSX(String downloadFilename, ProcessResponse<MedicalDocFile> info) {
        if (CollectionUtils.isEmpty(info.getProcessedFiles())) {
            return null;
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Результат");
            return convert(downloadFilename, workbook, sheet, info);
        } catch (Exception e) {
            log.error("Failed process files {}. Exception: {}", info.getProcessedFiles().size(), e.getMessage(), e);
            return null;
        }
    }
}
