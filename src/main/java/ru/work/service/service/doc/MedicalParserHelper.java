package ru.work.service.service.doc;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.springframework.stereotype.Component;
import ru.work.service.anotations.SheetColumn;
import ru.work.service.dto.ConvertDocToXlsx;
import ru.work.service.dto.FileDto;
import ru.work.service.dto.enums.ProcessedStatus;
import ru.work.service.dto.medical.MedicalDocFile;

import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.split;

@Slf4j
@Component
public class MedicalParserHelper implements ConvertDocToXlsx<MedicalDocFile> {

    @Override
    public MedicalDocFile readDoc(FileDto file) {
        try (
                InputStream inputStream = new FileInputStream(file.getAbsolutePath());
                HWPFDocument doc = new HWPFDocument(inputStream);
                WordExtractor we = new WordExtractor(doc)) {
            String template = clearParagraphsToText(we.getText()).toString();
            List<String> templateRows = Arrays.stream(split(template, SEPARATOR_RN)).toList();

            int partIndex = 0;
            Map<Integer, List<String>> parts = new HashMap<>();
            for (String row : templateRows) {
                if (row.equalsIgnoreCase(SEPARATOR_T)) {
                    partIndex++;
                    continue;
                }
                if (CollectionUtils.isEmpty(parts.get(partIndex))) {
                    parts.put(partIndex, new LinkedList<>());
                }
                parts.get(partIndex).add(row);
            }

            MedicalDocFile info = new MedicalDocFile(file);
            if (parts.isEmpty()) {
                info.setFailed(ProcessedStatus.FILE_NO_TEMPLATE, null);
                return info;
            }

            String outDateLine = parts.get(4).stream()
                    .map(l -> split(l, SEPARATOR_T))
                    .flatMap(Arrays::stream)
                    .filter(l -> StringUtils.containsAny(l, ".2024", ".2025", ".2026", ".2027", ".2028", ".2029"))
                    .findFirst().orElse(null);
            if (StringUtils.isNotBlank(outDateLine)) {
                info.setOutMaterialDate(clearSpaces(outDateLine));
            }

            if (CollectionUtils.isNotEmpty(parts.get(0))) {
                info.setHeader(clearSpaces(parts.get(0).get(0)));
            }

            if (CollectionUtils.isNotEmpty(parts.get(1))) {
                info.setSubHeader(clearSpaces(parts.get(1).get(0)));
            }

            List<Field> sheetFields = Arrays.stream(info.getClass().getDeclaredFields())
                    .filter(field -> field.getAnnotation(SheetColumn.class) != null)
                    .toList();
            for (String row : parts.get(1)) {
                row = row.replace(SEPARATOR_TR, "");
                String[] objects = split(row, SEPARATOR_T);
                if (objects.length > 1) {
                    var key = clearSpaces(objects[0]);
                    var value = clearSpaces(objects[1]);
                    Field sheetColumn = sheetFields.stream()
                            .filter(field -> {
                                SheetColumn a = field.getAnnotation(SheetColumn.class);
                                return a.name().equalsIgnoreCase(key) && a.enabled();
                            }).findFirst().orElse(null);
                    if (sheetColumn != null) {
                        String setterName = "set" + StringUtils.capitalize(sheetColumn.getName());
                        Class<?> type = sheetColumn.getType();
                        Method setter = info.getClass().getMethod(setterName, type);
                        SheetColumn a = sheetColumn.getAnnotation(SheetColumn.class);
                        if (type.equals(LocalDate.class)) {
                            setter.invoke(info, LocalDate.parse(value, DateTimeFormatter.ofPattern(a.parseFromFormat())));
                        } else {
                            setter.invoke(info, value);
                        }
                    }
                }
            }

            log.info("Добавляем микроорганизмы для <{}>", info.getFilename());
            for (String row : parts.get(2)) {
                String[] params = split(row, SEPARATOR_T);
                if (params.length == 3 && params[0].startsWith("[")) {
                    var number = params[0];
                    var name = clearSpaces(params[1]);
                    var count = clearSpaces(params[2]);
                    if (StringUtils.isNotBlank(name)) {
                        info.addMicroorganism(name, count);
                        log.debug("Микроорганизм: {} {} {}", number, info.getMicroorganisms().getLast().name, info.getMicroorganisms().getLast().count);
                    }
                }
            }

            if (info.getMicroorganisms().isEmpty()) {
                String errorMessage = "Файл <%s> не подходит. Таблица с микроорганизмами не заполнена. Размер файла %s КБ"
                        .formatted(info.getFilename(), file.getSizeKb());
                info.setFailed(ProcessedStatus.MEDICAL_FILE_IS_EMPTY, errorMessage);
                return info;
            }
            if (info.getMicroorganisms().get(0).name.equalsIgnoreCase("Pоста микрофлоры не обнаружено")) {
                String errorMessage = "Файл <%s> не подходит. Pоста микрофлоры не обнаружено. Размер файла %s КБ"
                        .formatted(info.getFilename(), file.getSizeKb());
                info.setFailed(ProcessedStatus.MEDICAL_FILE_IS_EMPTY, errorMessage);
                return info;
            }

            log.info("Добавляем антибиотикограмму для <{}>", info.getFilename());
            String header = null;
            for (String row : parts.get(3)) {
                if (StringUtils.containsAny(row, "[1]", "[2]", "[3]", "[4]", "**", "не имеет диагностического") ||
                    StringUtils.isBlank(row)) {
                    continue;
                }
                String[] params = split(row, SEPARATOR_T);
                if (params.length == 1) {
                    header = params[0];
                    info.addAntibioticGram(header);
                } else {
                    if (params.length == 2) {
                        var first = params[0].trim();
                        var second = params[1].trim();
                        if (first.equalsIgnoreCase(second)) {
                            log.trace("Это баг. Задвоился header: {}", params[0]);
                            continue;
                        }
                    }
                    String[] results = Arrays.stream(Arrays.copyOfRange(params, 1, params.length))
                            .map(this::clearSpaces)
                            .toArray(String[]::new);

                    String name = clearDoubleSpaces(params[0]);
                    info.addAntibioticGramItem(header, name, results);
                }
            }
            if (CollectionUtils.isEmpty(info.getAntibioticGrams())) {
                info.setFailed(ProcessedStatus.ANTI_NOT_FOUND, "Файл <%s> не подходит. Антибиотикограмма пуста. Размер файла %s КБ"
                        .formatted(info.getFilename(), file.getSizeKb()));
                return info;
            }

            info.setStatus(ProcessedStatus.SUCCESS);
            return info;
        } catch (Exception e) {
            MedicalDocFile info = new MedicalDocFile(file);
            String errorMessage = "Не удалось обработать файл <%s>: %s".formatted(file.getFilename(), e.getMessage());
            info.setFailed(ProcessedStatus.FAILED_PROCESS, errorMessage);
            return info;
        }
    }
}
