package ru.work.service.view.util;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

public class TimeFormatter {

    /**
     * Преобразует время в миллисекундах в читаемый формат, автоматически определяя,
     * какие части (часы, минуты, секунды) нужно выводить.
     *
     * @param durationInMillis Время в миллисекундах.
     * @return Строка в формате "3 час(а) 5 минут 12 секунд".
     */
    public static String formatDuration(long durationInMillis) {
        long hours = TimeUnit.MILLISECONDS.toHours(durationInMillis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(durationInMillis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(durationInMillis) % 60;

        StringBuilder result = new StringBuilder();

        if (hours > 0) {
            result.append(hours).append(" час").append(getPluralSuffix(hours)).append(" ");
        }

        if (minutes > 0 || hours > 0) {
            result.append(minutes).append(" минут").append(getPluralSuffix(minutes)).append(" ");
        }

        if (seconds > 0 || minutes > 0 || hours > 0) {
            result.append(seconds).append(" секунд").append(getPluralSuffix(seconds));
        }

        return result.toString().trim();
    }

    /**
     * Возвращает правильное окончание для слова в зависимости от числа.
     *
     * @param number Число, для которого нужно определить окончание.
     * @return Окончание: "а" для чисел 2-4, "" для остальных.
     */
    private static String getPluralSuffix(long number) {
        if (number % 10 >= 2 && number % 10 <= 4 && (number % 100 < 10 || number % 100 >= 20)) {
            return "а";
        }
        return "";
    }

    /**
     * Возвращает название месяца из даты
     *
     * @param date дата, для которой нужно определить месяц
     * @return месяц в печатном виде
     */
    public static String getMonth(LocalDate date) {
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
