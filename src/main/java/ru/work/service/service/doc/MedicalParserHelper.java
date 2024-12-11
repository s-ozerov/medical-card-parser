package ru.work.service.service.doc;

import lombok.RequiredArgsConstructor;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.containsAny;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.remove;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.split;
import static org.apache.commons.lang3.StringUtils.substring;

@Slf4j
@Component
public class MedicalParserHelper implements ConvertDocToXlsx<MedicalDocFile> {

    private static final String I = "I";
    private static final String R = "R";
    private static final String S = "S";
    private static final String X = "-";
    private static final String[] PARAMS = new String[]{I, R, S, X};

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
                if (row.equalsIgnoreCase(SEPARATOR_T) || row.equalsIgnoreCase("*" + SEPARATOR_T)) {
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

            if (parts.get(4) != null) {
                String outDateLine = parts.get(4).stream()
                        .map(l -> split(l, SEPARATOR_T))
                        .flatMap(Arrays::stream)
                        .filter(l -> containsAny(l, ".2024", ".2025", ".2026", ".2027", ".2028", ".2029"))
                        .findFirst().orElse(null);
                if (StringUtils.isNotBlank(outDateLine)) {
                    info.setOutMaterialDate(clearSpaces(outDateLine));
                }
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
                            value = value.replace("..", ".");
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

            if (parts.get(3) == null) {
                info.setFailed(ProcessedStatus.ANTI_NOT_FOUND, "Файл <%s> не подходит. Антибиотикограмма пуста. Размер файла %s КБ"
                        .formatted(info.getFilename(), file.getSizeKb()));
                return info;
            }

            log.info("Добавляем антибиотикограмму для <{}>", info.getFilename());
            String header = null;
            boolean isSuccessPart3 = true;
            for (String row : parts.get(3)) {
                if (containsAny(row, "[1]", "[2]", "[3]", "[4]", "**", "не имеет диагностического") ||
                    isBlank(row)) {
                    continue;
                }
                String[] params = split(row, SEPARATOR_T);
                if (params.length == 1) {
                    if (StringUtils.startsWithAny(row, "-", "S", "R", "I")) {
                        isSuccessPart3 = false;
                        break;
                    }
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
            if (!isSuccessPart3) {
                List<String> rows = processAntibioticGramsV2(parts.get(3), info);
                if (CollectionUtils.isEmpty(rows)) {
                    info.setFailed(ProcessedStatus.ANTI_NOT_FOUND, "Файл <%s> не подходит. Антибиотикограмма пуста. Размер файла %s КБ"
                            .formatted(info.getFilename(), file.getSizeKb()));
                    return info;
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
            ProcessedStatus status = ProcessedStatus.FAILED_PROCESS;
            if (e instanceof IllegalArgumentException) {
                status = ProcessedStatus.WRONG_CODING;
            }
            MedicalDocFile info = new MedicalDocFile(file);
            String errorMessage = "Не удалось обработать файл <%s>: %s".formatted(file.getFilename(), e.getMessage());
            info.setFailed(status, errorMessage);
            return info;
        }
    }

    private List<String> processAntibioticGramsV2(List<String> rows, MedicalDocFile info) {
        info.getAntibioticGrams().clear();

        try {
            AtomicReference<String> header = new AtomicReference<>();
            AtomicReference<String> first = new AtomicReference<>();
            LinkedList<String> filteredRows = rows.stream()
                    .filter(r -> {
                        if (isBlank(r) || containsAny(r, "[1]", "[2]", "[3]")) {
                            return false;
                        } else if (containsAny(r, "[4]")) {
                            GramInfo gramInfo = prepare(r);
                            if (gramInfo != null) {
                                header.set(gramInfo.header);
                                first.set(gramInfo.element);
                            }
                            return false;
                        } else {
                            return true;
                        }
                    })
                    .map(this::clearSpaces)
                    .collect(Collectors.toCollection(LinkedList::new));
            if (first.get() == null || header.get() == null) {
                return Collections.emptyList();
            }
            filteredRows.add(0, header.get());
            filteredRows.add(1, first.get());

            log.info("Prepare antibiotic gram by template v2 - {}", info.getFilename());
            LinkedList<String> delimRowList = new LinkedList<>();
            for (String row : filteredRows) {
                int l = row.length();
                if (l > 4) {
                    String start = substring(row, 0, 4);
                    String end = substring(row, l - 4, l);
                    if (containsAny(end, PARAMS)) {
                        List<String> splitWords = new LinkedList<>();
                        char[] chars = row.toCharArray();
                        StringBuilder builder = new StringBuilder();
                        for (int i = 0; i < chars.length; i++) {
                            if (i != 0 && Character.isUpperCase(chars[i]) && !(containsAny(String.valueOf(chars[i]), PARAMS))) {
                                splitWords.add(builder.toString());
                                builder = new StringBuilder();
                                builder.append(chars[i]);
                            } else if (i >= chars.length - 4 && containsAny(String.valueOf(chars[i]), PARAMS)) {
                                splitWords.add(builder.toString());
                                String params = row.substring(i, chars.length);
                                for (Character c : params.toCharArray()) {
                                    if (StringUtils.isNotBlank(String.valueOf(c))) {
                                        splitWords.add(String.valueOf(c));
                                    }
                                }
                                builder = null;
                                break;
                            } else {
                                builder.append(chars[i]);
                            }
                        }

                        if (builder != null) {
                            splitWords.add(builder.toString());
                        }
                        delimRowList.addAll(splitWords);
                    } else {
                        String subRow = row;
                        if (containsAny(start, PARAMS)) {
                            char[] chars = row.toCharArray();
                            int index = 0;
                            String[] _params = Arrays.copyOf(PARAMS, PARAMS.length + 1);
                            _params[PARAMS.length] = " ";
                            while (containsAny(String.valueOf(chars[index]), _params)) {
                                if (chars[index] != ' ') {
                                    delimRowList.add(String.valueOf(chars[index]));
                                }
                                index++;
                            }
                            subRow = row.substring(index);
                        }

                        List<String> splitWords = new LinkedList<>();
                        char[] chars = subRow.toCharArray();
                        StringBuilder builder = new StringBuilder();
                        for (int i = 0; i < chars.length; i++) {
                            if (i != 0 && Character.isUpperCase(chars[i])) {
                                if (chars[i] == 'М' && chars[i + 1] == 'П' && chars[i + 2] == 'К') {
                                    builder.append(chars[i]).append(chars[i + 1]).append(chars[i + 2]);
                                    i = i + 2;
                                    continue;
                                }
                                splitWords.add(builder.toString());
                                builder = new StringBuilder();
                                builder.append(chars[i]);
                            } else {
                                builder.append(chars[i]);
                            }
                            if (i == chars.length - 1) {
                                splitWords.add(builder.toString());
                            }
                        }
                        delimRowList.addAll(splitWords);
                    }
                } else {
                    if (containsAny(row, PARAMS)) {
                        for (Character c : row.toCharArray()) {
                            if (StringUtils.isNotBlank(String.valueOf(c))) {
                                delimRowList.add(String.valueOf(c));
                            }
                        }
                    } else {
                        delimRowList.add(row);
                    }
                }
            }

            return delimRowList;
        } catch (Exception e) {
            log.error("Failed to parse antibiotic gram V2: %s. ".formatted(e.getMessage()), e);
            return Collections.emptyList();
        }
    }

    private GramInfo prepare(String row) {
        char[] chars = row.toCharArray();
        int last = -1;
        for (int i = 0; i < chars.length; i++) {
            if (Character.isUpperCase(chars[i])) {
                last = i;
            }
        }
        if (last != -1) {
            String firstRow = row.substring(last, chars.length);
            String header = clearSpaces(remove(removeEnd(row, firstRow), "[4]"));
            return new GramInfo(header, firstRow);
        } else {
            log.error("Failed template V2: {}", row);
            return null;
        }
    }

    @RequiredArgsConstructor
    private static class GramInfo {
        public final String header;
        public final String element;
    }
}
