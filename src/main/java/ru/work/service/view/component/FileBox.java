package ru.work.service.view.component;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static ru.work.service.dto.enums.ProcessedStatus.ANTI_NOT_FOUND;
import static ru.work.service.dto.enums.ProcessedStatus.FAILED_PROCESS;
import static ru.work.service.dto.enums.ProcessedStatus.FAILED_READ;
import static ru.work.service.dto.enums.ProcessedStatus.FILE_NO_TEMPLATE;
import static ru.work.service.dto.enums.ProcessedStatus.MEDICAL_FILE_IS_EMPTY;
import static ru.work.service.view.util.StageUtil.setWidthAndHeight;

public class FileBox {

    public static void displayFileInfo(MedicalDocFile file) {
        Platform.runLater(() -> {
            Stage window = new Stage();

            window.initModality(Modality.APPLICATION_MODAL);
            window.setTitle(file.getFilename());
            setWidthAndHeight(window, 650, 800);
            window.getIcons().add(new Image(Constants.MAIN_ICO));

            TextArea textArea = new TextArea();
            textArea.setMinHeight(600);
            textArea.setMaxHeight(700);
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
                int size = file.getAntibioticGrams().get(0).items.get(0).size;
                String value = "[1]";
                if (size == 2) {
                    value = "[1]\t[2]";
                }
                if (size == 3) {
                    value = "[1]\t[2]\t[3]";
                }
                if (size == 4) {
                    value = "[1]\t[2]\t[3]\t[4]";
                }
                builder.append("%s\tАнтибиотикограмма".formatted(value)).append("\n");
                for (AntibioticGram gram : file.getAntibioticGrams()) {
                    builder.append(gram.header).append("\n");
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

    public static void displayFilesInfo(ProcessResponse<MedicalDocFile> groupFiles) {
        Platform.runLater(() -> {
            List<MedicalDocFile> completed = CollectionUtils.isEmpty(groupFiles.getProcessedFiles()) ?
                    new ArrayList<>() : groupFiles.getProcessedFiles();
            List<MedicalDocFile> errors = CollectionUtils.isEmpty(groupFiles.getErrorFiles()) ?
                    new ArrayList<>() : groupFiles.getErrorFiles();

            Stage window = new Stage();

            window.initModality(Modality.APPLICATION_MODAL);
            window.setTitle("Информация по обработке всех файлов");
            setWidthAndHeight(window, 400, 400);
            window.getIcons().add(new Image(Constants.MAIN_ICO));

            TextArea textArea = new TextArea();
            textArea.setMinHeight(300);
            textArea.setMaxHeight(380);
            textArea.setEditable(false);
            textArea.setWrapText(true);

            Map<ProcessedStatus, List<MedicalDocFile>> errorsMap;
            if (CollectionUtils.isEmpty(errors)) {
                errorsMap = null;
            } else {
                errorsMap = errors.stream()
                        .collect(Collectors.groupingBy(FileDto::getStatus));
            }

            Set<String> uniqBioMaterial = completed.stream()
                    .sorted(Comparator.comparing(MedicalDocFile::getBioMaterial))
                    .map(MedicalDocFile::getBioMaterial)
                    .collect(Collectors.toSet());

            StringBuilder builder = new StringBuilder();
            builder.append("Выжимка по успешным файлам").append("\n");
            addLine(builder, "Успешно обработано", completed.size());
            addLine(builder, "Кол-во результирующих строк для вставки", groupFiles.getCountForProcess());
            addLine(builder, "Используемый биоматериал, уникальные", uniqBioMaterial.size());
            emptyLine(builder);

            builder.append("Выжимка по неподходящим файлам [%s]".formatted(errors.size())).append("\n");
            addLine(builder, errorsMap, MEDICAL_FILE_IS_EMPTY);
            addLine(builder, errorsMap, ANTI_NOT_FOUND);
            addLine(builder, errorsMap, FILE_NO_TEMPLATE);
            addLine(builder, errorsMap, FAILED_READ);
            addLine(builder, errorsMap, FAILED_PROCESS);
            emptyLine(builder);
            addLine(builder, "Всего обработано файлов", completed.size() + errors.size());

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

    private static void addLine(StringBuilder builder, String key, String value) {
        if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
            builder.append(key).append(": ").append(value).append("\n");
        }
    }

    private static void addLine(StringBuilder builder, String key, Integer value) {
        builder.append(key).append(": ").append(value).append("\n");
    }

    private static void addLine(StringBuilder builder,
                                Map<ProcessedStatus, List<MedicalDocFile>> errorsMap,
                                ProcessedStatus status) {
        if (errorsMap == null || CollectionUtils.isEmpty(errorsMap.get(status))) {
            return;
        }
        builder.append("[").append(status.getMessage()).append("]").append(": ")
                .append(errorsMap.get(status).size()).append("\n");
    }

    private static void emptyLine(StringBuilder builder) {
        builder.append("\r\n");
    }
}
