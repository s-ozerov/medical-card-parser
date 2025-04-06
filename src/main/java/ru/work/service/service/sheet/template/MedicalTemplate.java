package ru.work.service.service.sheet.template;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import ru.work.service.dto.FileDto;
import ru.work.service.dto.ProcessResponse;
import ru.work.service.dto.medical.AntibioticGram;
import ru.work.service.dto.medical.MedicalDocFile;
import ru.work.service.dto.medical.MedicalSettingsDto;
import ru.work.service.service.doc.DocTemplate;
import ru.work.service.service.doc.MedicalDocReader;
import ru.work.service.service.sheet.SheetStyle;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static ru.work.service.service.sheet.SheetStyle.createHeaderTableCellsStyle;
import static ru.work.service.service.sheet.SheetStyle.createStandardTableCellsStyle;
import static ru.work.service.view.util.Constants.SMALL_FILE_SIZE;
import static ru.work.service.view.util.TimeFormatter.getMonth;

@Slf4j
@Component
@RequiredArgsConstructor
public class MedicalTemplate implements SheetTemplate<MedicalDocFile, MedicalSettingsDto>, DocTemplate<MedicalDocFile> {

    private final MedicalDocReader parserHelper;

    private final Map<Sheet, XSSFDrawing> drawingMap = new HashMap<>();

    @Override
    public ProcessResponse<MedicalDocFile> read(List<FileDto> files, ProgressBar progressBar, Label loadingText) {
        if (CollectionUtils.isEmpty(files)) {
            return new ProcessResponse<>();
        }

        AtomicInteger completed = new AtomicInteger(1);
        AtomicInteger countSmall = new AtomicInteger(0);
        List<MedicalDocFile> docFiles = files.stream()
                .map(file -> {
                    MedicalDocFile doc = parserHelper.read(file);
                    if (doc != null && doc.getSizeKb().compareTo(SMALL_FILE_SIZE) < 0) {
                        countSmall.set(countSmall.get() + 1);
                    }
                    if (progressBar != null) {
                        var progress = (double) completed.getAndIncrement() / files.size();

                        Platform.runLater(() -> {
                            progressBar.setProgress(progress);
                            var percent = BigDecimal.valueOf((double) completed.get() / files.size() * 100)
                                                  .setScale(2, RoundingMode.DOWN) + "%";
                            loadingText.setText(percent + " (%d / %d)".formatted(completed.get(), files.size()));
                        });
                    }
                    return doc;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        log.info("Файлов с размеров меньше {} кб: {}", SMALL_FILE_SIZE, countSmall.get());
        return new ProcessResponse<>(docFiles);
    }

    @Override
    public void buildRowHeaders(XSSFWorkbook workbook, Sheet sheet, MedicalSettingsDto settings) {
        XSSFDrawing drawing = drawingMap.get(sheet);
        if (drawing == null) {
            drawing = ((XSSFSheet) sheet).createDrawingPatriarch();
            drawingMap.put(sheet, drawing);
        }

        CellStyle headerTableStyle = createHeaderTableCellsStyle(workbook);
        Row row = buildRow(sheet);

        var columnEnabled = settings.getColumnEnabled();
        if (columnEnabled.isMonth()) {
            addCell(row, "Месяц", headerTableStyle);
            SheetStyle.setLastCollWidth(sheet, 16);
        }

        if (columnEnabled.isMicroorganisms()) {
            addCell(row, "Бактерии", headerTableStyle);
            SheetStyle.setLastCollWidth(sheet, 48);
        }

        if (columnEnabled.isDivision()) {
            addCell(row, "Отделение", headerTableStyle);
            SheetStyle.setLastCollWidth(sheet, 16);
        }

        if (columnEnabled.isBioMaterial()) {
            addCell(row, "Биоматериал", headerTableStyle);
            SheetStyle.setLastCollWidth(sheet, 32);
        }

        if (columnEnabled.isReceiveMaterialDate()) {
            addCell(row, "Дата поступления материала", headerTableStyle);
            SheetStyle.setLastCollWidth(sheet, 48);
        }

        if (columnEnabled.isFilename()) {
            addCell(row, "Название файла", headerTableStyle);
            SheetStyle.setLastCollWidth(sheet, 48);
        }

        if (columnEnabled.isPatient()) {
            addCell(row, "Пациент", headerTableStyle);
            SheetStyle.setLastCollWidth(sheet, 48);
        }

        if (columnEnabled.isDiagnose()) {
            addCell(row, "Диагноз", headerTableStyle);
            SheetStyle.setLastCollWidth(sheet, 48);
        }

        if (columnEnabled.isIb()) {
            addCell(row, "ИБ", headerTableStyle);
            SheetStyle.setLastCollWidth(sheet, 48);
        }

        if (columnEnabled.isNumberAnalyze()) {
            addCell(row, "№ анализа", headerTableStyle);
            SheetStyle.setLastCollWidth(sheet, 48);
        }

        CreationHelper createHelper = sheet.getWorkbook().getCreationHelper();
        for (Map.Entry<String, List<String>> column : settings.getColumns().entrySet()) {
            String cellValue = column.getKey().replace("_", "/");
            Cell cell = addCellWithComment(createHelper, drawing, column.getValue().get(0), row, cellValue, headerTableStyle);
            SheetStyle.setLastCollWidthAuto(sheet);
        }
    }

    /**
     * @return Текст ошибки при обработке файла
     */
    @Override
    public String buildRowData(XSSFWorkbook workbook, Sheet sheet, MedicalDocFile doc, MedicalSettingsDto settings) {
        CellStyle style = createStandardTableCellsStyle(workbook);

        StringBuilder failed = new StringBuilder();
        for (int i = 0; i < doc.getMicroorganisms().size(); i++) {
            if (CollectionUtils.isEmpty(doc.getAntibioticGrams())) {
                continue;
            }

            List<AntibioticGram.AntibioticoGramItem> items = new LinkedList<>();
            for (AntibioticGram gram : doc.getAntibioticGrams()) {
                if (gram.items.isEmpty() || gram.items.get(0).size <= i) {
                    return null;
                }
                items.addAll(gram.items);
            }

            var columnEnabled = settings.getColumnEnabled();
            var row = sheet.createRow(sheet.getLastRowNum() + 1);
            addEnabledCell(columnEnabled.isMonth(), row, getMonth(doc.getReceiveMaterialDate()), style);
            addEnabledCell(columnEnabled.isMicroorganisms(), row, doc.getMicroorganisms().get(i).name, style);
            addEnabledCell(columnEnabled.isDivision(), row, doc.getDivision(), style);
            addEnabledCell(columnEnabled.isBioMaterial(), row, doc.getBioMaterial(), style);
            addEnabledCell(columnEnabled.isReceiveMaterialDate(), row, doc.getReceiveMaterialDate(), style);
            addEnabledCell(columnEnabled.isFilename(), row, doc.getFilename(), style);
            addEnabledCell(columnEnabled.isPatient(), row, doc.getPatient(), style);
            addEnabledCell(columnEnabled.isDiagnose(), row, doc.getDiagnose(), style);
            addEnabledCell(columnEnabled.isIb(), row, doc.getIb(), style);
            addEnabledCell(columnEnabled.isNumberAnalyze(), row, doc.getNumberAnalyze(), style);

            for (Map.Entry<String, List<String>> column : settings.getColumns().entrySet()) {
                AntibioticGram.AntibioticoGramItem anti = items.stream()
                        .filter(a -> column.getValue().stream().anyMatch(val -> val.equalsIgnoreCase(a.name)))
                        .findFirst().orElse(null);

                String cellValue = "";
                if (anti != null) {
                    cellValue = anti.result.get(i + 1);
                    items.remove(anti);
                }
                addCell(row, cellValue, style);
            }

            if (!CollectionUtils.isEmpty(items)) {
                log.error("ITEMS NOT EMPTY: {}", StringUtils.join(items.stream().map(item -> item.name).collect(Collectors.toSet())));
                failed.append(", ").append(StringUtils.join(items.stream().map(item -> item.name).collect(Collectors.toSet()), ", "));
            }
        }
        return failed.toString();
    }


}
