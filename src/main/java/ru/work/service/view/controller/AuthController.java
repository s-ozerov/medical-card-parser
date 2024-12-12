package ru.work.service.view.controller;

import com.github.plushaze.traynotification.notification.Notifications;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxLoadException;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.core.FxmlView;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import ru.work.service.config.MedicalTemplateProperties;
import ru.work.service.rest.CurrentTimeRestAdapter;
import ru.work.service.view.JavaFxApplication;
import ru.work.service.view.component.ExceptionBox;
import ru.work.service.view.util.ControllerUtil;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;

import static ru.work.service.view.JavaFxApplication.APPLICATION_CONTEXT;

@Slf4j
@Component
@RequiredArgsConstructor
@FxmlView("auth.fxml")
public class AuthController {

    public static final byte[] KEY_ENCRYPT = new byte[]{52, 53, 99, 52, 56, 56, 48, 55, 45, 98, 57, 49, 50, 45, 52, 53, 51,
            99, 45, 98, 52, 50, 56, 45, 57, 52, 57, 57, 100, 97, 100, 100, 57, 49, 102, 54};

    @FXML
    private Button authButton;

    @FXML
    private TextField authTextField;

    @FXML
    private Button exitButton;

    private final MedicalTemplateProperties properties;
    private final CurrentTimeRestAdapter currentTimeRestAdapter;
    private Boolean isAuth = null;

    @FXML
    public void auth(ActionEvent event) {
        if (isAuth != null && isAuth) {
            load();
            return;
        }

        String text = authTextField.getText();
        if (StringUtils.isBlank(text) || !Arrays.equals(text.getBytes(StandardCharsets.UTF_8), KEY_ENCRYPT)) {
            ControllerUtil.blockButton(authButton, 5);
            ControllerUtil.showAlert("Авторизация", "Не верный ключ", Notifications.WARNING);
            authTextField.clear();
        } else {
            load();
        }
    }

    @FXML
    public void exit(ActionEvent event) {
        Platform.exit();
        System.exit(0);
    }

    @FXML
    public void initialize() {
        LocalDateTime time = currentTimeRestAdapter.getCurrentTime();
        if (time == null || time.isAfter(
                LocalDateTime.of(
                        LocalDate.of(2024, 12, 28),
                        LocalTime.of(0, 0, 0)))) {
            authButton.setDisable(true);
            ExceptionBox.displayWarn("Информация", "Пробный период окончен!");
            return;
        } else {
            if (StringUtils.isNotBlank(properties.getKey())) {
                if (Arrays.equals(properties.getKey().getBytes(StandardCharsets.UTF_8), KEY_ENCRYPT)) {
                    authTextField.setVisible(false);
                    isAuth = true;
                } else {
                    isAuth = false;
                    ControllerUtil.showAlert("Авторизация", "Не верный ключ в настройках", Notifications.WARNING);
                }
            }
        }
    }

    private void load() {
        try {
            ControllerUtil.showAlert("Авторизация", "Успешная авторизация", Notifications.INFORMATION);
            FxWeaver fxWeaver = APPLICATION_CONTEXT.getBean(FxWeaver.class);
            Parent root = fxWeaver.loadView(MainController.class);
            Scene scene = new Scene(root);
            JavaFxApplication.WINDOW.setScene(scene);
        } catch (FxLoadException e) {
            log.error("Failed to load view MainController: {}", e.getMessage(), e);
            ControllerUtil.showAlert("Авторизация", "Ошибка на стороне сервера: %s".formatted(e.getMessage()), Notifications.ERROR);
        }
    }

}
