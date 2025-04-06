package ru.work.service.view.component;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import ru.work.service.dto.FileDto;
import ru.work.service.dto.ProcessResponse;
import ru.work.service.dto.enums.ProcessedStatus;
import ru.work.service.dto.medical.AntibioticGram;
import ru.work.service.dto.medical.MedicalDocFile;
import ru.work.service.dto.medical.Microorganism;
import ru.work.service.view.util.Constants;
import ru.work.service.view.util.ImageFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static ru.work.service.dto.enums.ProcessedStatus.ANTI_V1_FAILED;
import static ru.work.service.dto.enums.ProcessedStatus.ANTI_V1_IS_EMPTY;
import static ru.work.service.dto.enums.ProcessedStatus.ANTI_V2_FIRST_STEP;
import static ru.work.service.dto.enums.ProcessedStatus.ANTI_V2_SECOND_STEP;
import static ru.work.service.dto.enums.ProcessedStatus.FAILED_PROCESS;
import static ru.work.service.dto.enums.ProcessedStatus.FAILED_READ;
import static ru.work.service.dto.enums.ProcessedStatus.FILE_NO_TEMPLATE;
import static ru.work.service.dto.enums.ProcessedStatus.MEDICAL_FILE_IS_EMPTY;
import static ru.work.service.dto.enums.ProcessedStatus.WRONG_CODING;
import static ru.work.service.view.util.Constants.PATCH_CSS;
import static ru.work.service.view.util.ImageFactory.buildSize20;
import static ru.work.service.view.util.StageUtil.setWidthAndHeight;

public class FileBox {

    private static final Map<VBox, Boolean> expandedMap = new HashMap<>();

    public static void displayFileInfo(MedicalDocFile file) {
        Platform.runLater(() -> {
            Stage window = new Stage();

            window.initModality(Modality.APPLICATION_MODAL);
            window.setTitle(file.getFilename());
            setWidthAndHeight(window, 650, 700);
            window.getIcons().add(new Image(Constants.MAIN_ICO));

            TextArea textArea = new TextArea();
            setWidthAndHeight(textArea, 600, 600);
            textArea.setEditable(false);

            StringBuilder builder = new StringBuilder();
            addLine(builder, "Статус", file.getStatus().getMessage());
            if (StringUtils.isNotBlank(file.getErrorMessage())) {
                addLine(builder, "Ошибка", file.getErrorMessage());
            }
            emptyLine(builder);
            addLine(builder, "Заголовок", file.getHeader());
            addLine(builder, "Подзаголовок", file.getSubHeader());
            emptyLine(builder);
            if (file.getReceiveMaterialDate() != null) {
                addLine(builder, "Дата поступление материала", file.getReceiveMaterialDate().toString());
            }
            addLine(builder, "Пациент", file.getPatient());
            addLine(builder, "Биоматериал", file.getBioMaterial());
            addLine(builder, "Диагноз", file.getDiagnose());
            addLine(builder, "ИБ", file.getIb());
            addLine(builder, "№ анализа", file.getNumberAnalyze());
            addLine(builder, "Отделение", file.getDivision());
            if (!CollectionUtils.isEmpty(file.getMicroorganisms())) {
                emptyLine(builder);
                builder.append("Выделенные микроорганизмы - КОЕ/мл").append("\n");
                int i = 1;
                for (Microorganism micro : file.getMicroorganisms()) {
                    builder.append("[%s] - %s - %s\n".formatted(i, micro.name, micro.count));
                    i++;
                }
            }

            if (!CollectionUtils.isEmpty(file.getAntibioticGrams())) {
                emptyLine(builder);

                String value = getCountGram(file);
                builder.append("%s\tАнтибиотикограмма".formatted(value)).append("\n");
                for (AntibioticGram gram : file.getAntibioticGrams()) {
                    builder.append(gram.header).append("\n");
                    if (!CollectionUtils.isEmpty(gram.items)) {
                        for (int i = 0; i < gram.items.size(); i++) {
                            StringBuilder sb = new StringBuilder();
                            AntibioticGram.AntibioticoGramItem item = gram.items.get(i);
                            for (int j = 1; j <= item.result.size(); j++) {
                                sb.append(item.result.get(j)).append("\t");
                            }
                            builder.append("%s\t| %s".formatted(sb.toString(), item.name)).append("\n");
                        }
                    }
                }
            }

            if (StringUtils.isNotBlank(file.getOutMaterialDate())) {
                emptyLine(builder);
                addLine(builder, "Дата выдачи", file.getOutMaterialDate());
            }

            textArea.setText(builder.toString());

            Button closeButton = new Button("Закрыть");
            closeButton.setOnAction(e -> window.close());

            VBox layout = new VBox(10);
            layout.getChildren().addAll(textArea, closeButton);
            layout.setAlignment(Pos.CENTER);

            Scene scene = new Scene(layout);
            window.setScene(scene);
            window.showAndWait();
        });
    }

    private static String getCountGram(MedicalDocFile file) {
        String value = "";
        if (!CollectionUtils.isEmpty(file.getAntibioticGrams().get(0).items)) {
            int size = file.getAntibioticGrams().get(0).items.get(0).size;
            value = "[1]";
            if (size == 2) {
                value = "[1]\t[2]";
            }
            if (size == 3) {
                value = "[1]\t[2]\t[3]";
            }
            if (size == 4) {
                value = "[1]\t[2]\t[3]\t[4]";
            }
        }
        return value;
    }

    public static void displayFilesInfo(ProcessResponse<MedicalDocFile> groupFiles,
                                        Label countSuccessFilesLabel,
                                        Label countSuccessForInputLabel,
                                        Label countSuccessBioLabel,
                                        VBox  errorBox,
                                        VBox errorHandlerBox,
                                        Label countErrorFilesLabel) {
        Platform.runLater(() -> {
            List<MedicalDocFile> completed = CollectionUtils.isEmpty(groupFiles.getSuccessFiles()) ? new ArrayList<>() : groupFiles.getSuccessFiles();
            List<MedicalDocFile> errors = CollectionUtils.isEmpty(groupFiles.getErrorFiles()) ? new ArrayList<>() : groupFiles.getErrorFiles();

            Set<String> uniqBioMaterial = completed.stream()
                    .sorted(Comparator.comparing(MedicalDocFile::getBioMaterial))
                    .map(MedicalDocFile::getBioMaterial)
                    .collect(Collectors.toSet());

            countSuccessFilesLabel.setText(String.valueOf(completed.size()));
            countSuccessForInputLabel.setText(String.valueOf(groupFiles.getCountForProcess()));
            countSuccessBioLabel.setText(String.valueOf(uniqBioMaterial.size()));

            if (!CollectionUtils.isEmpty(errors)) {
                errorBox.setVisible(true);
                errorHandlerBox.getChildren().clear();

                countErrorFilesLabel.setText(String.valueOf(errors.size()));

                Map<ProcessedStatus, List<MedicalDocFile>> errorsMap = errors.stream().collect(Collectors.groupingBy(FileDto::getStatus));

                addTypedErrorMessage(errorHandlerBox, errorsMap, MEDICAL_FILE_IS_EMPTY, true);//f
                addTypedErrorMessage(errorHandlerBox, errorsMap, ANTI_V1_IS_EMPTY, true); //f
                addTypedErrorMessage(errorHandlerBox, errorsMap, ANTI_V1_FAILED, true);
                addTypedErrorMessage(errorHandlerBox, errorsMap, ANTI_V2_FIRST_STEP, true);//f
                addTypedErrorMessage(errorHandlerBox, errorsMap, ANTI_V2_SECOND_STEP, true);
                addTypedErrorMessage(errorHandlerBox, errorsMap, FILE_NO_TEMPLATE, true);//f
                addTypedErrorMessage(errorHandlerBox, errorsMap, FAILED_READ, true);
                addTypedErrorMessage(errorHandlerBox, errorsMap, FAILED_PROCESS, true);
                addTypedErrorMessage(errorHandlerBox, errorsMap, WRONG_CODING, true);

                errorHandlerBox.setVisible(true);
            }


        });
    }

    private static void addLine(StringBuilder builder, String key, String value) {
        if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
            builder.append(key).append(": ").append(value).append("\n");
        }
    }

    private static void addLine(StringBuilder builder, String key, Integer value) {
        builder.append(key).append(": ").append(value).append("\n");
    }

    private static void addTypedErrorMessage(VBox errorHandlerBox,
                                             Map<ProcessedStatus, List<MedicalDocFile>> errorsMap,
                                             ProcessedStatus status,
                                             Boolean isAddedFiles) {
        if (errorsMap == null || CollectionUtils.isEmpty(errorsMap.get(status))) {
            return;
        }

        Button toggleButton = new Button(/*"▼"*/);
        toggleButton.getStyleClass().add("toggle-button");
        setWidthAndHeight(toggleButton, 20, 20);
        toggleButton.setGraphic(buildSize20(ImageFactory.ImageName.OPEN));
        toggleButton.setStyle("""
                -fx-background-color: transparent;
                -fx-cursor: hand;
                """);

        ListView<String> listContent = new ListView<>();
        setWidthAndHeight(listContent, 430, 390);
        listContent.getStylesheets().add(PATCH_CSS + "standart-list.css");
        if (isAddedFiles) {
            for (MedicalDocFile doc : errorsMap.get(status)) {
                listContent.getItems().add(doc.getFilename());
            }
        } else {
            toggleButton.setVisible(false);
        }

        Label name = new Label(status.getMessage() + ": ");
        name.setStyle("""
                -fx-font-size: 14px;
                -fx-font-family: "Bookman Old Style";
                -fx-text-fill: #f47874
                """);
        Label count = new Label(String.valueOf(errorsMap.get(status).size()));
        count.setStyle("""
                -fx-font-size: 14px;
                -fx-font-family: "Verdana";
                """);
        HBox title = new HBox(10, name, count, toggleButton);

        VBox container = new VBox(10, title);
        errorHandlerBox.getChildren().addAll(container);

        toggleButton.setOnAction(e -> {
            Platform.runLater(() -> {
                Stage window = new Stage();

                window.initModality(Modality.APPLICATION_MODAL);
                window.setTitle(status.getMessage());
                setWidthAndHeight(window, 450, 450);
                window.getIcons().add(new Image(Constants.MAIN_ICO));

                VBox layout = new VBox(10);
                HBox box = new HBox(listContent);
                box.setAlignment(Pos.CENTER);
                layout.getChildren().addAll(box);

                Scene scene = new Scene(layout);
                window.setScene(scene);
                window.showAndWait();
            });
        });
    }

    private static void addLine(StringBuilder builder,
                                Map<ProcessedStatus, List<MedicalDocFile>> errorsMap,
                                ProcessedStatus status,
                                Boolean printFileNames) {
        if (errorsMap == null || CollectionUtils.isEmpty(errorsMap.get(status))) {
            return;
        }
        builder.append("[").append(status.getMessage()).append("]").append(": ")
                .append(errorsMap.get(status).size());
        if (printFileNames) {
            for (MedicalDocFile doc : errorsMap.get(status)) {
                builder.append("\n").append("\t-> ").append(doc.getFilename());
            }
        }
        builder.append("\n");
    }

    private static void emptyLine(StringBuilder builder) {
        builder.append("\r\n");
    }
}
