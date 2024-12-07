package ru.work.service.service.sheet.template;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import ru.work.service.config.MedicalTemplateProperties;
import ru.work.service.dto.DownloadDto;
import ru.work.service.dto.FileDto;
import ru.work.service.dto.ProcessResponse;
import ru.work.service.dto.medical.AntibioticGram;
import ru.work.service.dto.medical.MedicalDocFile;
import ru.work.service.service.doc.MedicalParserHelper;
import ru.work.service.service.sheet.SheetStyle;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static ru.work.service.helper.FileHelper.calculateSize;
import static ru.work.service.service.sheet.SheetStyle.createHeaderTableCellsStyle;
import static ru.work.service.service.sheet.SheetStyle.createStandardTableCellsStyle;

@Slf4j
@Component
@RequiredArgsConstructor
public class MedicalTemplate implements SheetTemplate<MedicalDocFile> {

    private final MedicalParserHelper parserHelper;
    private final MedicalTemplateProperties properties;

    @Getter
    private final Map<String, List<AntibioticGram.AntibioticoGramItem>> notFound = new HashMap<>();

    @Override
    public ProcessResponse<MedicalDocFile> read(List<FileDto> files) {
        if (CollectionUtils.isEmpty(files)) {
            return new ProcessResponse<>();
        }

        List<MedicalDocFile> docFiles = files.stream()
                .map(parserHelper::readDoc)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        notFound.clear();
        return new ProcessResponse<>(docFiles);
    }

    @Override
    public DownloadDto prepare(String downloadFilename, ByteArrayOutputStream xlsxContent) {
        if (!CollectionUtils.isEmpty(notFound)) {
            for (Map.Entry<String, List<AntibioticGram.AntibioticoGramItem>> entry : notFound.entrySet()) {
                if (!CollectionUtils.isEmpty(entry.getValue())) {
                    log.error("Не удалось найти колонки для <{}>: {}", entry.getKey(), StringUtils.join(entry.getValue().stream().map(s -> s.name).collect(Collectors.toSet()), ","));
                }
            }
        }

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

    @Override
    public void buildRowHeaders(XSSFWorkbook workbook, Sheet sheet) {
        CellStyle headerTableStyle = createHeaderTableCellsStyle(workbook);
        var row = sheet.createRow(sheet.getLastRowNum() + 1);

        addCell(row, "Месяц", headerTableStyle);
        SheetStyle.setLastCollWidth(sheet, 16);

        addCell(row, "Бактерии", headerTableStyle);
        SheetStyle.setLastCollWidth(sheet, 48);

        addCell(row, "Отделение", headerTableStyle);
        SheetStyle.setLastCollWidth(sheet, 16);

        addCell(row, "Биоматериал", headerTableStyle);
        SheetStyle.setLastCollWidth(sheet, 32);

        if (properties.getColumnEnabled().getFilename()) {
            addCell(row, "Название файла", headerTableStyle);
            SheetStyle.setLastCollWidth(sheet, 48);
        }

        for (Map.Entry<String, List<String>> column : properties.getColumns().entrySet()) {
            String cellValue = column.getKey().replace("_", "/");
            addCell(row, cellValue, headerTableStyle);
            SheetStyle.setLastCollWidthAuto(sheet);
        }
    }

    @Override
    public void buildRowData(XSSFWorkbook workbook, Sheet sheet, MedicalDocFile doc) {
        CellStyle cellTableStyle = createStandardTableCellsStyle(workbook);

        for (int i = 0; i < doc.getMicroorganisms().size(); i++) {
            if (CollectionUtils.isEmpty(doc.getAntibioticGrams())) {
                continue;
            }

            List<AntibioticGram.AntibioticoGramItem> items = new LinkedList<>();
            for (AntibioticGram gram : doc.getAntibioticGrams()) {
                if (gram.items.isEmpty() || gram.items.get(0).size <= i) {
                    return;
                }
                items.addAll(gram.items);
            }

            var row = sheet.createRow(sheet.getLastRowNum() + 1);
            addCell(row, month(doc.getReceiveMaterialDate()), cellTableStyle);
            addCell(row, doc.getMicroorganisms().get(i).name, cellTableStyle);
            addCell(row, doc.getDivision(), cellTableStyle);
            addCell(row, doc.getBioMaterial(), cellTableStyle);

            if (properties.getColumnEnabled().getFilename()) {
                addCell(row, doc.getFilename(), cellTableStyle);
            }

            for (Map.Entry<String, List<String>> column : properties.getColumns().entrySet()) {
                AntibioticGram.AntibioticoGramItem anti = items.stream()
                        .filter(a -> column.getValue().stream().anyMatch(val -> val.equalsIgnoreCase(a.name)))
                        .findFirst().orElse(null);

                String cellValue = "";
                if (anti != null) {
                    cellValue = anti.result.get(i + 1);
                    items.remove(anti);
                }
                addCell(row, cellValue, cellTableStyle);
            }

            if (!CollectionUtils.isEmpty(items)) {
                notFound.put(doc.getFilename(), items);
            }
        }
    }

    private static String month(LocalDate date) {
        return switch (date.getMonth()) {
            case JANUARY -> "Январь";
            case FEBRUARY -> "Февраль";
            case MARCH -> "Март";
            case APRIL -> "Апрель";
            case MAY -> "Май";
            case JUNE -> "Июнь";
            case JULY -> "Июль";
            case AUGUST -> "Август";
            case SEPTEMBER -> "Сентябрь";
            case OCTOBER -> "Октябрь";
            case NOVEMBER -> "Ноябрь";
            case DECEMBER -> "Декабрь";
        };
    }

}
