package ru.work.service.view.factory;

import javafx.application.Platform;
import javafx.scene.control.ListView;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.work.service.view.util.LogStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Formatter;

public class LogFactory {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM HH:mm:ss");

    private final Logger log;
    private final ListView<String> view;

    public LogFactory(Class<?> controllerClass, ListView<String> view) {
        log = LoggerFactory.getLogger(controllerClass);
        this.view = view;
    }

    public synchronized void info(String message) {
        print(LogStatus.INFO, message);
        log.info("{}", message);
    }

    public synchronized void info(String message, Object... args) {
        if (args == null || ObjectUtils.allNull(args)) {
            print(LogStatus.INFO, message);
        } else {
            message = new Formatter().format(message, args).toString();
            print(LogStatus.INFO, message);
        }
        log.info("{}", message);
    }

    public synchronized void error(String message) {
        print(LogStatus.ERROR, message);
        log.error("{}", message);
    }

    public synchronized void error(String message, Object... args) {
        if (args == null || ObjectUtils.allNull(args)) {
            print(LogStatus.ERROR, message);
        } else {
            message = new Formatter().format(message, args).toString();
            print(LogStatus.ERROR, message);
        }
        log.error("{}", message);
    }

    private synchronized void print(LogStatus status, String message) {
        Platform.runLater(() -> {
            if (view.getItems().size() > 1000) {
                var array = view.getItems();
                view.getItems().clear();
                view.getItems().addAll(array.subList(800, 1000));
            }
            String date = LocalDateTime.now().format(formatter);
            view.getItems().add(date + " " + status + ":  " + message);
        });
    }

}
