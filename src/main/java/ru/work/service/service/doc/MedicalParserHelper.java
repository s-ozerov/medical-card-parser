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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.containsAny;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.split;
import static org.apache.commons.lang3.StringUtils.substring;
import static ru.work.service.view.util.Constants.SMALL_FILE_SIZE;

@Slf4j
@Component
public class MedicalParserHelper implements ConvertDocToXlsx<MedicalDocFile> {

    private static final String I = "I";
    private static final String R = "R";
    private static final String S = "S";
    private static final String X = "-";
    private static final String IX = "I/-";
    private static final String RX = "R/-";
    private static final String SX = "S/-";
    private static final String XI = "-/I";
    private static final String XR = "-/R";
    private static final String XS = "-/S";
    private static final String XX = "-/-";
    private static final String II = "I/I";
    private static final String RR = "R/R";
    private static final String SS = "S/S";
    private static final int sizeISRX = 3;
    private static final String NONE = "IRS-/";//new char[]{'I', 'R', 'S', '-', '/'};
    private static final String[] PARAMS = new String[]{I, R, S, X, IX, RX, SX, XI, XR, XS, XX, II, RR, SS};
    private static final String[] DRAFT_WORDS = new String[]{"[1]", "[2]", "[3]", "[4]", "[5]", "**",
            "не имеет диагностического", "*Определение чувствительности", "Дата выдачи"};

    @Override
    public MedicalDocFile readDoc(FileDto file) {
        try (
                InputStream inputStream = new FileInputStream(file.getAbsolutePath());
                HWPFDocument doc = new HWPFDocument(inputStream);
                WordExtractor we = new WordExtractor(doc)) {
            String template = clearParagraphsToText(we.getText()).toString();
            List<String> templateRows = Arrays.stream(split(template, SEPARATOR_RN)).toList();

            Map<Integer, List<String>> parts = getParts(templateRows);

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
                if (isNotBlank(outDateLine)) {
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

            for (String row : parts.get(2)) {
                String[] params = split(row, SEPARATOR_T);
                if (params.length == 3 && params[0].startsWith("[")) {
                    var name = clearSpaces(params[1]);
                    var count = clearSpaces(params[2]);
                    if (isNotBlank(name)) {
                        info.addMicroorganism(name, count);
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
                //TODO проверить сами файлы, что в них
                if (info.getSizeKb().compareTo(SMALL_FILE_SIZE) < 0) {
                    log.info("<{}> Антибиотикограмма в parts пустая", info.getFilename());
                } else {
                    log.error("<{}> Антибиотикограмма в parts пустая. Размер файла: {}", info.getFilename(), info.getSizeKb());
                }

                info.setFailed(ProcessedStatus.ANTI_V1_IS_EMPTY, "Файл <%s> не подходит. Антибиотикограмма пуста. Размер файла %s КБ"
                        .formatted(info.getFilename(), file.getSizeKb()));
                return info;
            }

            boolean isFailedParse = false;
            for (String row : parts.get(3)) {
                row = row.replace(" ", "");
                char[] chars = row.toCharArray();
                for (int i = 0; i < chars.length - 2; i++) {
                    if (StringUtils.containsAny(String.valueOf(chars[i]), NONE) &&
                        StringUtils.containsAny(String.valueOf(chars[i + 1]), NONE) &&
                        StringUtils.containsAny(String.valueOf(chars[i + 2]), NONE)) {
                        isFailedParse = true;
                    }
                    if (isFailedParse) break;
                }
                if (isFailedParse) break;
            }

            boolean failedFirstTemplate = false;
            if (!isFailedParse) {
                log.info("Шаблон 1. Антибиотикограмма - {}", info.getFilename());
                String header = null;
                try {
                    for (String row : parts.get(3)) {
                        if (containsAny(row, DRAFT_WORDS) ||
                            isBlank(row)) {
                            continue;
                        }
                        String[] params = split(row, SEPARATOR_T);
                        if (params.length == 1) {
                            if (StringUtils.startsWithAny(row, X, S, R, I)) {
                                failedFirstTemplate = true;
                                break;
                            }
                            header = clearSpaces(clearDoubleSpaces(params[0]));
                        } else {
                            if (params.length == 2) {
                                var first = params[0].trim();
                                var second = params[1].trim();
                                if (first.equalsIgnoreCase(second)) {
                                    log.trace("Это баг. Задвоился header: {}", params[0]);
                                    continue;
                                }
                            }
                            LinkedList<String> results = Arrays.stream(Arrays.copyOfRange(params, 1, params.length))
                                    .map(this::clearSpaces)
                                    .map(String::toUpperCase)
                                    .collect(Collectors.toCollection(LinkedList::new));

                            String name = clearSpaces(clearDoubleSpaces(params[0]));
                            info.addAntibioticGramItem(header, name, results);
                        }
                    }
                } catch (Exception e) {
                    log.error("Шаблон 1 - ошибка. Антибиотикограмма - <{}>: {}", info.getFilename(), e.getMessage());
                    info.setFailed(ProcessedStatus.ANTI_V1_FAILED, "Файл <%s> не подходит. Ошибка обработки: %s".formatted(info.getFilename(), e.getMessage()));
                    return info;
                }
            } else {
                try {
                    log.info("Пробуем ещё раз. Не удачно отработан шаблон 1 для файла: {}", info.getFilename());
                    List<String> rowsV2 = Arrays.stream(we.getParagraphText())
                            .map(r -> clearSpaces(r.trim()))
                            .filter(r -> isNotBlank(r) && !containsAny(r, "[1]", "[2]", "[3]", "[4]", "[5]", "[6]", "[7]"))
                            .toList();
                    if (!rowsV2.isEmpty()) {
                        List<String> antiGramInfo = new ArrayList<>();
                        boolean added = false;
                        for (String s : rowsV2) {
                            if (StringUtils.startsWithIgnoreCase(s, "антибиотикограмма")) {
                                added = true;
                                continue;
                            }
                            if (StringUtils.startsWithIgnoreCase(s, "дата выдачи")) {
                                break;
                            }
                            if (added) {
                                antiGramInfo.add(s);
                            }
                        }
                        if (!antiGramInfo.isEmpty()) {
                            fillAntiGram(antiGramInfo, info);
                            log.info("Удачная повторная попытка для шаблона 1");
                        }
                    }
                } catch (Exception e) {
                    log.error("failed: {}", e.getMessage());
                    failedFirstTemplate = true;
                }
            }

            if (failedFirstTemplate) {
                //TODO проверить Колистин,К ≤ 1мг/л почему-то пропадает МПК
                log.info("Шаблон 2. Антибиотикограмма - {}", info.getFilename());
                info.getAntibioticGrams().clear();
                List<String> antiGramInfo = processAntibioticGramsV2(parts.get(3));
                if (CollectionUtils.isEmpty(antiGramInfo)) {
                    info.setFailed(ProcessedStatus.ANTI_V2_FIRST_STEP, "Файл <%s> не подходит. Антибиотикограмма пустая".formatted(info.getFilename()));
                    return info;
                } else {
                    try {
                        fillAntiGram(antiGramInfo, info);
                    } catch (Exception e) {
                        log.error("Шаблон 2. Антибиотикограмма - <{}>: {}", info.getFilename(), e.getMessage());
                        info.setFailed(ProcessedStatus.ANTI_V2_SECOND_STEP, "Файл <%s> не подходит. Ошибка обработки: %s".formatted(info.getFilename(), e.getMessage()));
                        return info;
                    }
                }
            }

            if (CollectionUtils.isEmpty(info.getAntibioticGrams())) {
                info.setFailed(ProcessedStatus.ANTI_V1_IS_EMPTY, "Файл <%s> не подходит. Антибиотикограмма пуста. Размер файла %s КБ"
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

    private Map<Integer, List<String>> getParts(Collection<String> template) {
        int partIndex = 0;
        Map<Integer, List<String>> parts = new HashMap<>();
        for (String row : template) {
            if (row.equalsIgnoreCase(SEPARATOR_T) || row.equalsIgnoreCase("*" + SEPARATOR_T)) {
                partIndex++;
                continue;
            }
            if (CollectionUtils.isEmpty(parts.get(partIndex))) {
                parts.put(partIndex, new LinkedList<>());
            }
            parts.get(partIndex).add(row);
        }
        return parts;
    }

    private List<String> processAntibioticGramsV2(List<String> rows) {
        try {
            LinkedList<String> filteredRows = rows.stream()
                    .map(r -> r.replace("*", ""))
                    .map(this::clearSpaces)
                    .filter(r -> isNotBlank(r) && !containsAny(r, "[1]", "[2]", "[3]"))
                    .collect(Collectors.toCollection(LinkedList::new));

            LinkedList<String> delimRowList = new LinkedList<>();
            for (String row : filteredRows) {
                if (containsAny(row, "4]")) {
                    row = row.substring(row.lastIndexOf("]") + 1);
                }
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
                                    if (isNotBlank(String.valueOf(c))) {
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
                            if (isNotBlank(String.valueOf(c))) {
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

    private static void fillAntiGram(List<String> rows, MedicalDocFile info) {
        String aHeader = null;
        String aName = null;
        LinkedList<String> results = new LinkedList<>();
        for (int i = 0; i < rows.size() - 1; i++) {
            String row = rows.get(i);
            if (rows.get(i).length() > sizeISRX && rows.get(i + 1).length() > sizeISRX) {
                aHeader = row;
                continue;
            } else if (rows.get(i).length() > sizeISRX) {
                aName = row;
                continue;
            }

            results.add(row);
            if (i + 1 == rows.size() || rows.get(i + 1).length() > sizeISRX || !containsAny(rows.get(i + 1), PARAMS)) {
                info.addAntibioticGramItem(aHeader, aName, results);
                results = new LinkedList<>();
            }
        }
    }

}
