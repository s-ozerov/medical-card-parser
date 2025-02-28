package ru.work.service.view.controller;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxmlView;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import ru.work.service.dto.DownloadDto;
import ru.work.service.dto.FileDto;
import ru.work.service.dto.ProcessResponse;
import ru.work.service.dto.enums.Extension;
import ru.work.service.dto.enums.ProcessedStatus;
import ru.work.service.dto.medical.MedicalDocFile;
import ru.work.service.helper.FileHelper;
import ru.work.service.service.sheet.MedicalFileHandler;
import ru.work.service.view.JavaFxApplication;
import ru.work.service.view.component.DownloadComponent;
import ru.work.service.view.component.ExceptionBox;
import ru.work.service.view.component.FileBox;
import ru.work.service.view.factory.LogFactory;
import ru.work.service.view.util.ControllerUtil;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static ru.work.service.dto.enums.Extension.DOC;
import static ru.work.service.dto.enums.Extension.DOCX;
import static ru.work.service.view.util.Constants.CURRENT_THEME;
import static ru.work.service.view.util.Theme.DARK;
import static ru.work.service.view.util.Theme.LIGHT;

@Slf4j
@Component
@RequiredArgsConstructor
@FxmlView("main.fxml")
public class MainController {

    @FXML
    private ListView<String> logView;
    @FXML
    private ListView<MedicalDocFile> fileList;

    @FXML
    private Button startButton;
    @FXML
    private Button openButton;
    @FXML
    private Button openMultipleButton;
    @FXML
    private Button clearLogButton;
    @FXML
    private Button downloadButton;
    @FXML
    private Button showErrorsButton;

    @FXML
    private Label countNameLabel;
    @FXML
    private Label countLabel;
    @FXML
    private ChoiceBox<String> themeBox;

    private final MedicalFileHandler medicalHandler;
    private final DownloadComponent downloadComponent;

    private LogFactory _log;
    private FileDto currentFileInfo;
    private String currentPath;

    private ProcessResponse<MedicalDocFile> processedFiles;
    private DownloadDto downloadDto;

    @FXML
    public void initialize() {
        setThemeClearButton();
        setThemeOpenButton();
        setThemeMultipleOpenButton();
        countNameLabel.setVisible(false);

        _log = new LogFactory(this.getClass(), logView);

        initButtons();
        initListView();
        ObservableList<String> themes = FXCollections.observableArrayList(DARK.name(), LIGHT.name());

        setThemeLogView(logView);
        setThemeSuperButton(startButton);
        setThemeIconButton(openButton);
        setThemeIconButton(openMultipleButton);
        setThemeDownloadButton();

        themeBox.setValue(CURRENT_THEME.name());
        themeBox.setItems(themes);
        themeBox.setOnAction(clickEvent -> {
            if (themeBox.getValue().equalsIgnoreCase(DARK.name())) {
                JavaFxApplication.setTheme(new PrimerDark());
                CURRENT_THEME = DARK;
            } else {
                JavaFxApplication.setTheme(new PrimerLight());
                CURRENT_THEME = LIGHT;
            }
            setThemeSuperButton(startButton);
            setThemeIconButton(openButton);
            setThemeIconButton(openMultipleButton);
            setThemeLogView(logView);
        });
        themeBox.getStylesheets().add("/ru/work/service/view/css/choice-box.css");
    }

    private void initListView() {
        fileList.setOnMouseClicked(click -> {
            if (click.getClickCount() != 2 || fileList.getItems().isEmpty()) {
                return;
            }
            MedicalDocFile file = fileList.getSelectionModel().getSelectedItem();
            FileBox.displayFileInfo(file);
            _log.info("Включен просмотр файла: " + file.toString());
        });
    }

    private void initButtons() {
        startButton.getStyleClass().add("super-button"); // Применяем стиль
        openButton.getStyleClass().add("icon-button"); // Применяем стиль
        openMultipleButton.getStyleClass().add("icon-button"); // Применяем стиль
        downloadButton.getStyleClass().add("download-button"); // Применяем стиль

        showErrorsButton.setVisible(false);
        showErrorsButton.setOnAction(e -> {
            if (processedFiles == null) {
                showErrorsButton.setVisible(false);
                return;
            }
            FileBox.displayFilesInfo(processedFiles);
            _log.info("Включен просмотр обработки файла(ов).");
        });

        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выбор файла");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(DOC.name(), DOC.filter),
                new FileChooser.ExtensionFilter(DOCX.name(), DOCX.filter),
                new FileChooser.ExtensionFilter("Все файлы", "*.*")
        );

        startButton.setDisable(true);

        openButton.setOnAction(e -> {
            File file = fileChooser.showOpenDialog(JavaFxApplication.WINDOW);
            if (file != null) {
                String filename = file.getName();
                String path = file.getAbsolutePath();

                logDelim();

                _log.info("Путь к файлу <%s>", path);
                _log.info("Имя файла: %s", filename);

                Extension extension = Extension.fromFile(filename);
                if (!Extension.isDOC(extension)) {
                    startButton.setDisable(true);
                    downloadButton.setDisable(true);
                    _log.error("Не верный формат файла [%s]", Extension.get(filename));
                    file = null;
                } else if (CURRENT_THEME == LIGHT) {
                    startButton.setDisable(false);
                    downloadButton.setDisable(true);
                } else {
                    startButton.setDisable(false);
                    downloadButton.setDisable(true);
                }

                FileDto currentFileInfo = FileHelper.buildFileInfo(file);
                if (currentFileInfo != null) {
                    this.currentPath = null;
                    if (currentFileInfo.getStatus() == ProcessedStatus.FAILED_READ) {
                        ExceptionBox.displayWarn("Ошибка чтения", "Не удалось обработать файл. Ошибка: " + currentFileInfo.getErrorMessage());
                    } else {
                        this.fileList.getItems().clear();
                        countLabel.setText("");
                        countNameLabel.setVisible(false);
                        showErrorsButton.setVisible(false);

                        startButton.setDisable(false);
                        startButton.setText("Получить из файла");
                        this.currentFileInfo = currentFileInfo;
                    }
                }
            }
        });

        final DirectoryChooser directoryChooser = new DirectoryChooser();
        openMultipleButton.setOnAction(e -> {
            final File selectedDirectory = directoryChooser.showDialog(JavaFxApplication.WINDOW);
            directoryChooser.setTitle("Выбор папки с файлами");
            directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            if (selectedDirectory != null) {
                this.currentFileInfo = null;
                this.fileList.getItems().clear();
                countNameLabel.setVisible(false);
                countLabel.setText("");
                showErrorsButton.setVisible(false);

                logDelim();

                _log.info("Найдена папка: %s", selectedDirectory.getAbsolutePath());
                this.currentPath = selectedDirectory.getPath();
                startButton.setDisable(false);
                startButton.setText("Получить из папки");
                downloadButton.setDisable(true);
            }
        });

        clearLogButton.setOnAction(e -> {
            logView.getItems().clear();
            ControllerUtil.blockButton(clearLogButton, 5);
        });

        startButton.setOnAction(e -> Platform.runLater(() -> {
            fileList.getItems().clear();
            if (isNull(currentFileInfo)) {
                if (StringUtils.isBlank(currentPath)) {
                    ExceptionBox.displayWarn("Ошибка", "Ошибка запуска операции. Файл или папка не выбраны");
                    _log.error("Ошибка запуска операции. Файл или папка не выбраны");
                } else {
                    _log.info("Поиск файлов на чтение по пути <%s>", currentPath);
                    BiConsumer<ProgressBar, Label> startProcess = this::startProcessReadFiles;
                    Runnable processSuccess = this::processReadFilesSuccess;
                    Runnable processFailed = this::processReadFilesFailed;

                    try {
                        downloadComponent.startReadFiles(startProcess, processSuccess, processFailed);
                    } catch (Exception exception) {
                        log.error("Failed to process files: {}", currentPath, exception);
                    }
                }
            } else {
                startButton.setDisable(true);
                downloadButton.setDisable(true);
                _log.info("Поиск на чтение файла <%s> ", currentFileInfo.getFilename());

                this.processedFiles = medicalHandler.readFile(currentFileInfo);
                if (CollectionUtils.isEmpty(processedFiles.getSuccessFiles())) {
                    _log.error("Файл не удовлетворяет фильтрации <%s>. Контент не поддерживается.", currentFileInfo.getAbsolutePath());
                } else {
                    setFiles(processedFiles);
                    this.downloadDto = null;
                    downloadButton.setDisable(false);
                    _log.info("Чтение успешно для файла <%s>", currentFileInfo.getFilename());
                }
                showErrorsButton.setVisible(true);
                startButton.setDisable(false);
            }
        }));

        FileChooser chooserForSave = new FileChooser();
        chooserForSave.setTitle("Сохранение результата");
        downloadButton.setDisable(true);
        downloadButton.setOnAction(e -> Platform.runLater(() -> {
            if (processedFiles != null && !CollectionUtils.isEmpty(processedFiles.getSuccessFiles())) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH-mm-ss-SSS");
                String downloadFilename = "результат " + formatter.format(LocalDateTime.now()) + Extension.XLSX.format();
                chooserForSave.setInitialFileName(downloadFilename);
                File saveFile = chooserForSave.showSaveDialog(JavaFxApplication.WINDOW);
                if (nonNull(saveFile)) {
                    if (this.downloadDto != null) {
                        try {
                            FileUtils.copyInputStreamToFile(this.downloadDto.getContent(), saveFile);
                            _log.info("Файл повторно сохранён <%s>", saveFile.getName());
                        } catch (IOException ex) {
                            _log.error("Не удалось сохранить файл <%s>. Ошибка: %s", saveFile.getName(), ex.getMessage());
                        }
                        return;
                    }
                    _log.info("Преобразуем содержимое файла/файлов в XLSX формат по пути <%s>", saveFile.getAbsolutePath());
                    DownloadDto downloadDto = medicalHandler.convertDOCToXLSX(downloadFilename, processedFiles);
                    try {
                        if (downloadDto == null) {
                            _log.error("Не удалось сохранить файл <%s>. Ошибка: %s", saveFile.getName(), "выбранный doc файл не обработан");
                        } else {
                            FileUtils.copyInputStreamToFile(downloadDto.getContent(), saveFile);
                            _log.info("Файл успешно преобразован и сохранён <%s>", saveFile.getName());
                            if (!CollectionUtils.isEmpty(downloadDto.getNotFound())) {
                                for (Map.Entry<String, String> entry : downloadDto.getNotFound().entrySet()) {
                                    if (!StringUtils.isNotBlank(entry.getValue())) {
                                        _log.error("Не найдены колонки <%s>: %s", entry.getKey(), entry.getValue());
                                    }
                                }
                            }
                        }
                    } catch (IOException ex) {
                        _log.error("Не удалось сохранить файл <%s>. Ошибка: %s", saveFile.getName(), ex.getMessage());
                    }
                }
            } else {
                _log.error("Не удалось получить файлы для скачивания");
                downloadButton.setDisable(true);
            }
        }));
    }

    private void startProcessReadFiles(ProgressBar progressBar, Label label) {
        openButton.setDisable(true);
        openMultipleButton.setDisable(true);
        startButton.setDisable(true);
        downloadButton.setDisable(true);
        processedFiles = medicalHandler.readFiles(currentPath, progressBar, label);
    }

    private void processReadFilesSuccess() {
        log.info("Success processed files: {}", currentPath);
        if (processedFiles == null || CollectionUtils.isEmpty(processedFiles.getSuccessFiles())) {
            _log.error("Не найдены файлы удовлетворяющие фильтры <%s>", currentPath);
        } else {
            setFiles(processedFiles);
            downloadDto = null;
            downloadButton.setDisable(false);
            _log.info("Чтение успешно по пути <%s>", currentPath);
        }
        disabledProcessReadFiles();
    }

    private void processReadFilesFailed() {
        log.info("Failed processed files: {}", currentPath);
        disabledProcessReadFiles();
    }

    private void disabledProcessReadFiles() {
        openButton.setDisable(false);
        openMultipleButton.setDisable(false);
        startButton.setDisable(false);
        showErrorsButton.setVisible(true);
    }

    private void setFiles(ProcessResponse<MedicalDocFile> res) {
        List<MedicalDocFile> result = Stream.concat(res.getSuccessFiles().stream(), res.getErrorFiles().stream())
                .sorted(Comparator.comparing(MedicalDocFile::getFilename))
                .toList();
        fileList.getItems().addAll(result);
        countNameLabel.setVisible(true);
        countLabel.setText(String.valueOf(fileList.getItems().size()));
    }

    private void logDelim() {
        if (!logView.getItems().isEmpty()) {
            _log.info("=======================================================================");
        }
    }

    private void setThemeDownloadButton() {
        Image icon = new Image(this.getClass().getResourceAsStream("/image/download-32.png"));
        ImageView iconView = new ImageView(icon);
        iconView.setFitWidth(36);
        iconView.setFitHeight(36);
        downloadButton.setGraphic(iconView);

        String styleSheet = "/ru/work/service/view/css/button-download.css";
        downloadButton.getStylesheets().add(styleSheet);
    }

    private void setThemeOpenButton() {
        Image icon = new Image(this.getClass().getResourceAsStream("/image/word.png"));
        ImageView iconView = new ImageView(icon);
        iconView.setFitWidth(36);
        iconView.setFitHeight(36);
        openButton.setGraphic(iconView);
    }

    private void setThemeMultipleOpenButton() {
        Image icon = new Image(this.getClass().getResourceAsStream("/image/folder-48.png"));
        ImageView iconView = new ImageView(icon);
        iconView.setFitWidth(36);
        iconView.setFitHeight(36);
        openMultipleButton.setGraphic(iconView);
    }

    private void setThemeClearButton() {
        Image icon = new Image(this.getClass().getResourceAsStream("/image/delete-icon.png"));
        ImageView iconView = new ImageView(icon);
        iconView.setFitWidth(48);
        iconView.setFitHeight(48);
        clearLogButton.setGraphic(iconView);
        clearLogButton.setStyle("""
                -fx-background-color: transparent;
                -fx-cursor: hand;
                """);
    }

    private static void setThemeLogView(ListView<String> logView) {
        logView.getStylesheets().clear();
        if (CURRENT_THEME == LIGHT) {
            String styleSheet = "/ru/work/service/view/css/log-list-light.css";
            logView.getStylesheets().add(styleSheet);
        } else {
            String styleSheet = "/ru/work/service/view/css/log-list-dark.css";
            logView.getStylesheets().add(styleSheet);
        }
    }

    private static void setThemeSuperButton(Button button) {
        button.getStylesheets().clear();
        if (CURRENT_THEME == LIGHT) {
            String styleSheet = "/ru/work/service/view/css/button-super-light.css";
            button.getStylesheets().add(styleSheet);
        } else {
            String styleSheet = "/ru/work/service/view/css/button-super-dark.css";
            button.getStylesheets().add(styleSheet);
        }
    }

    private static void setThemeIconButton(Button button) {
        button.getStylesheets().clear();
        if (CURRENT_THEME == LIGHT) {
            String styleSheet = "/ru/work/service/view/css/button-icon-light.css";
            button.getStylesheets().add(styleSheet);
        } else {
            String styleSheet = "/ru/work/service/view/css/button-icon-dark.css";
            button.getStylesheets().add(styleSheet);
        }
    }

}
