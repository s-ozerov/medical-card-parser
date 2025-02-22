package ru.work.service.view.component;

import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.BiConsumer;

import static ru.work.service.view.util.Constants.CURRENT_THEME;
import static ru.work.service.view.util.Theme.LIGHT;

@Slf4j
@Component
@RequiredArgsConstructor
public class DownloadComponent {

    public void startReadFiles(@NonNull BiConsumer<ProgressBar, Label> startProcess,
                               @NonNull Runnable processSuccess,
                               @NonNull Runnable processFailed) {
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefSize(600, 20);

        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(2);
        dropShadow.setOffsetX(2);
        dropShadow.setOffsetY(2);
        progressBar.setEffect(dropShadow);

        Label progressLabel = new Label("0%");

        setProgressBar(progressBar, progressLabel);

        StackPane window = new StackPane();
        window.getChildren().addAll(progressBar, progressLabel);
        window.setStyle("""
                -fx-background-radius: 20;
                -fx-border-radius: 20;
                -fx-border-color: #31ab37;
                -fx-border-width: 1;
                """);
        StackPane.setAlignment(progressLabel, Pos.CENTER);

        Scene scene = new Scene(window, 620, 38);
        scene.setFill(null);

        Stage stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setScene(scene);
        stage.setAlwaysOnTop(true);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Progress");
        stage.show();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                startProcess.accept(progressBar, progressLabel);
                return null;
            }
        };

        task.setOnSucceeded(event -> {
            processSuccess.run();
            stage.close();
        });

        task.setOnFailed(event -> {
            processFailed.run();
            stage.close();
        });

        new Thread(task).start();
    }

    private static void setProgressBar(ProgressBar progressBar, Label progressBarText) {
        String styleSheet;
        if (CURRENT_THEME == LIGHT) {
            styleSheet = "/ru/work/service/view/css/download-light.css";
        } else {
            styleSheet = "/ru/work/service/view/css/download-dark.css";
        }

        progressBarText.getStylesheets().clear();
        progressBarText.getStylesheets().add(styleSheet);

        progressBar.getStylesheets().clear();
        progressBar.getStylesheets().add(styleSheet);
    }

}
