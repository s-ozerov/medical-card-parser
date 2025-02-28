package ru.work.service.exception;

public class FailedSaveSettingsException extends RuntimeException {
    public FailedSaveSettingsException(String message) {
        super(message);
    }
}
