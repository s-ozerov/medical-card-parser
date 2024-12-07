package ru.work.service.view.util;


import com.github.plushaze.traynotification.animations.Animations;
import com.github.plushaze.traynotification.notification.Notifications;
import com.github.plushaze.traynotification.notification.TrayNotification;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.paint.Paint;
import javafx.util.Duration;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ru.work.service.view.component.BlockButton;

import static ru.work.service.view.util.Constants.MAIN_ICO;

@Slf4j
public class ControllerUtil {

    public static void blockButton(Button button, Integer sec) {
        newThread("blockButton", new BlockButton(button, sec));
    }

    @SneakyThrows
    public static void showAlert(String title, String message, Notifications notification) {
        TrayNotification tray = new TrayNotification(title, message, notification);
        tray.setAnimation(Animations.POPUP);
        tray.setRectangleFill(Paint.valueOf("#000000"));
        tray.setImage(new Image(MAIN_ICO));
        tray.showAndDismiss(Duration.seconds(3));
    }

    private static void newThread(String threadName, Runnable runnable) {
        new Thread(runnable, threadName).start();
    }
}
