package ru.work.service.service.sheet;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import ru.work.service.dto.DownloadDto;
import ru.work.service.dto.ProcessResponse;
import ru.work.service.dto.medical.MedicalDocFile;
import ru.work.service.dto.medical.MedicalSettingsDto;
import ru.work.service.service.manager.MedicalSettingsManager;
import ru.work.service.service.sheet.template.MedicalTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import static ru.work.service.helper.FileHelper.calculateSize;

/**
 * Сервис для работы с файлами расширения .xls и .xlsx
 */
@Slf4j
@Service
public class MedicalFileHandler extends AbstractFileHandler<MedicalDocFile> {

    private final MedicalTemplate medicalTemplate;

    @Autowired
    public MedicalFileHandler(MedicalTemplate medicalTemplate) {
        super(medicalTemplate);
        this.medicalTemplate = medicalTemplate;
    }

    public DownloadDto convertDOCToXLSX(String downloadFilename, ProcessResponse<MedicalDocFile> files) {
        if (files == null) {
            return null;
        }

        var successFiles = files.getSuccessFiles();
        if (CollectionUtils.isEmpty(successFiles)) {
            return null;
        }

        final MedicalSettingsDto settings = MedicalSettingsManager.readSettings();
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Результат");

            medicalTemplate.buildRowHeaders(workbook, sheet, settings);

            final Map<String, String> notFound = new HashMap<>();
            for (MedicalDocFile file : successFiles) {
                var error = medicalTemplate.buildRowData(workbook, sheet, file, settings);
                if (StringUtils.isNotBlank(error)) {
                    notFound.put(file.getFilename(), error);
                }
            }

            try (ByteArrayOutputStream xlsxContent = new ByteArrayOutputStream()) {
                workbook.write(xlsxContent);
                DownloadDto content = download(downloadFilename, xlsxContent);
                content.setNotFound(notFound);
                return content;
            }
        } catch (Exception e) {
            log.error("Failed process files {}. Exception: {}", successFiles.size(), e.getMessage(), e);
            return null;
        }
    }

    @Override
    protected DownloadDto download(String downloadFilename, ByteArrayOutputStream xlsxContent) {
        byte[] bytes = xlsxContent.toByteArray();
        DownloadDto download = DownloadDto.builder()
                .filename(downloadFilename)
                .content(new ByteArrayInputStream(bytes))
                .sizeKb(calculateSize(bytes.length))
                .build();

        try {
            xlsxContent.close();
            return download;
        } catch (Exception e) {
            log.error("Failed download file {}. Exception: {}", downloadFilename, e.getMessage(), e);
            return DownloadDto.builder()
                    .filename(downloadFilename).build();
        }
    }
}
