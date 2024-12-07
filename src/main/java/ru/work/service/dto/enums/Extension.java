package ru.work.service.dto.enums;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

@RequiredArgsConstructor
public enum Extension {

    DOC("*.doc"),
    DOCX("*.docx"),
    PDF("*.pdf"),
    XLSX("*.xlsx");

    public final String filter;

    public String format() {
        return "." + this.name().toLowerCase();
    }

    public static String get(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return null;
        }

        int start = fileName.lastIndexOf(".");
        if (start == -1) {
            return null;
        }

        String extensionFromFile = fileName.substring(start + 1);
        if (StringUtils.isBlank(extensionFromFile)) {
            return null;
        }
        return extensionFromFile;
    }

    public static Extension fromFile(String fileName) {
        String extensionFromFile = get(fileName);
        if (StringUtils.isBlank(extensionFromFile)) {
            return null;
        }

        return Arrays.stream(values())
                .filter(e -> e.name().equalsIgnoreCase(extensionFromFile))
                .findFirst()
                .orElse(null);
    }

    public static boolean isDOC(Extension extension) {
        return extension == DOC || extension == DOCX;
    }
}
