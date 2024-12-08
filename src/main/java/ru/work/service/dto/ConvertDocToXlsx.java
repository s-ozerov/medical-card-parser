package ru.work.service.dto;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

public interface ConvertDocToXlsx<T> {

    String SEPARATOR_TR = "\t\r";
    String SEPARATOR_T = "\t";
    String SEPARATOR_RN = "\r\n";

    T readDoc(FileDto dto);

    default StringBuilder clearParagraphsToText(String text) {
        StringBuilder builder = new StringBuilder(text);
        for (int i = 0; i < builder.length(); i++) {
            if (builder.charAt(0) == '\r' || builder.charAt(0) == '\n') {
                builder.deleteCharAt(0);
            } else {
                break;
            }
        }
        return builder;
    }

    default String clearSpaces(String paragraph) {
        if (StringUtils.isBlank(paragraph)) return null;
        return paragraph.trim().replace("\u0007", "").replace("  ", "");
    }

    /**
     * Удаляет лишние пробелы между словами
     */
    default String clearDoubleSpaces(String text) {
        return StringUtils.join(Arrays.stream(text.split(" "))
                .filter(StringUtils::isNotBlank)
                .toList(), " ");
    }
}
