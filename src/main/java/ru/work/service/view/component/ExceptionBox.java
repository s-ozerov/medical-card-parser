package ru.work.service.view.component;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ru.work.service.view.util.Constants;

public class ExceptionBox {

    public static void displayWarn(String title, String message) {
        Platform.runLater(() -> {
            Stage window = new Stage();

            window.initModality(Modality.APPLICATION_MODAL);
            window.setTitle(title);
            window.setMinWidth(400);
            window.setMinHeight(140);
            window.setMaxHeight(140);
            window.getIcons().add(new Image(Constants.WARN_ICO));

            Label label = new Label();
            label.setText(message);
            Button closeButton = new Button("Закрыть");
            closeButton.setOnAction(e -> window.close());

            VBox layout = new VBox(10);
            layout.getChildren().addAll(label, closeButton);
            layout.setAlignment(Pos.CENTER);

            Scene scene = new Scene(layout);
            window.setScene(scene);
            window.showAndWait();
        });
    }
}
