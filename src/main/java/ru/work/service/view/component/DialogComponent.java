package ru.work.service.view.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class DialogComponent {

    private String text = null;

    private final String title;
    private final String label;

    public DialogComponent(String title, String label) {
        this.title = title;
        this.label = label;
    }

    public void show() {
        Stage inputStage = new Stage();
        inputStage.initModality(Modality.APPLICATION_MODAL);
        inputStage.setTitle(title);

        TextField inputField = new TextField();
        inputField.setPromptText("Введите текст");

        VBox layout = buildForm(inputField, inputStage);
        inputStage.setScene(new Scene(layout, 500, 150));
        inputStage.setAlwaysOnTop(true);
        inputStage.showAndWait();
    }

    private VBox buildForm(TextField inputField, Stage inputStage) {
        Button submitButton = new Button("Подтвердить");
        submitButton.setOnAction(e -> {
            String inputText = inputField.getText();
            if (!inputText.isEmpty()) {
                log.info("Введённый данные: {}", inputText);
                inputStage.close();
                this.text = inputText;
            } else {
                log.info("Поле ввода пустое!");
                this.text = null;
            }
        });
        HBox hBox = new HBox(submitButton);
        hBox.setAlignment(Pos.CENTER);
        VBox layout = new VBox(10, new Label(label), inputField, hBox);
        layout.setPadding(new Insets(10));
        return layout;
    }

    public String getText() {
        return StringUtils.isBlank(text) ? null : text.trim();
    }
}
