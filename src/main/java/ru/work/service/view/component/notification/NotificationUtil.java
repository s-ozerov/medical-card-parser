package ru.work.service.view.component.notification;

import com.github.plushaze.traynotification.animations.Animations;
import com.github.plushaze.traynotification.notification.Notifications;
import com.github.plushaze.traynotification.notification.TrayNotification;
import javafx.scene.image.Image;
import javafx.scene.paint.Paint;
import javafx.util.Duration;
import lombok.SneakyThrows;

import static ru.work.service.view.util.Constants.DOWNLOAD_ICO;

public class NotificationUtil {

    @SneakyThrows
    public static void showAlert(String title, String message, Notifications notification) {
        TrayNotification tray = new TrayNotification(title, message, notification);
        tray.setAnimation(Animations.FADE);
        tray.setRectangleFill(Paint.valueOf("#000000"));
        tray.setImage(new Image(DOWNLOAD_ICO));
        tray.showAndDismiss(Duration.seconds(5));
    }

}
