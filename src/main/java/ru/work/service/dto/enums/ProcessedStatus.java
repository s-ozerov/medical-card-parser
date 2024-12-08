package ru.work.service.dto.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProcessedStatus {

    MEDICAL_FILE_IS_EMPTY("Рост микрофлоры не обнаружен"),
    WRONG_CODING("Не верная кодировка файла"),
    WRONG_TABLE("""
            Не верно составлена таблица:
            'Антибиотикограмма' - не удалось разделить столбцы и строки
            """),
    ANTI_NOT_FOUND("Антибиотикограмма не заполнена"),
    FILE_NO_TEMPLATE("Файл не подходит под шаблон"),
    FAILED_READ("Не удалось прочитать файл"),
    FAILED_PROCESS("Не удачная обработка файла"),
    COMPLETE_READ("Файл прочитан"),
    SUCCESS("Успешная обработка файла");

    private final String message;
}
