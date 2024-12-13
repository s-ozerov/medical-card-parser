package ru.work.service.view.controller;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import com.gluonhq.charm.glisten.control.ProgressBar;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import lombok.RequiredArgsConstructor;
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
import ru.work.service.dto.medical.AntibioticGram;
import ru.work.service.dto.medical.MedicalDocFile;
import ru.work.service.helper.FileHelper;
import ru.work.service.service.sheet.MedicalSheetFileHandler;
import ru.work.service.view.JavaFxApplication;
import ru.work.service.view.component.ExceptionBox;
import ru.work.service.view.component.FileBox;
import ru.work.service.view.factory.LogFactory;
import ru.work.service.view.util.ControllerUtil;
import ru.work.service.view.util.StageUtil;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static ru.work.service.dto.enums.Extension.DOC;
import static ru.work.service.dto.enums.Extension.DOCX;
import static ru.work.service.view.util.Constants.CURRENT_THEME;
import static ru.work.service.view.util.Theme.DARK;
import static ru.work.service.view.util.Theme.LIGHT;

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
    @FXML
    private ProgressBar loading;

    private final MedicalSheetFileHandler medicalHandler;

    private volatile LogFactory _log;
    private FileDto currentFileInfo;
    private String currentPath;

    private ProcessResponse<MedicalDocFile> processedFiles;
    private DownloadDto downloadDto;

    @FXML
    public void initialize() {
        countNameLabel.setVisible(false);

        StageUtil.setWidthAndHeight(JavaFxApplication.WINDOW, 950, 590);
        _log = new LogFactory(this.getClass(), logView);

        initButtons();
        initListView();
        ObservableList<String> themes = FXCollections.observableArrayList(DARK.name(), LIGHT.name());

        setLogView(logView);

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
            setLogView(logView);
        });
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
                downloadButton.setDisable(true);
            }
        });

        clearLogButton.setOnAction(e -> {
            logView.getItems().clear();
            ControllerUtil.blockButton(clearLogButton, 5);
        });

        startButton.setOnAction(e -> Platform.runLater(() -> {
            startButton.setDisable(true);
            downloadButton.setDisable(true);
            if (isNull(currentFileInfo)) {
                if (StringUtils.isBlank(currentPath)) {
                    ExceptionBox.displayWarn("Ошибка", "Ошибка запуска операции. Файл или папка не выбраны");
                    _log.error("Ошибка запуска операции. Файл или папка не выбраны");
                } else {
                    _log.info("Поиск файлов на чтение по пути <%s>", currentPath);
                    this.processedFiles = medicalHandler.readFile(currentPath);
                    if (CollectionUtils.isEmpty(processedFiles.getProcessedFiles())) {
                        _log.error("Не найдены файлы удовлетворяющие фильтры <%s>", currentPath);
                    } else {
                        setFiles(processedFiles);
                        this.downloadDto = null;
                        downloadButton.setDisable(false);
                        _log.info("Чтение успешно по пути <%s>", currentPath);
                    }
                    showErrorsButton.setVisible(true);
                }
            } else {
                _log.info("Поиск на чтение файла <%s> ", currentFileInfo.getFilename());
                this.processedFiles = medicalHandler.readFile(currentFileInfo);
                if (CollectionUtils.isEmpty(processedFiles.getProcessedFiles())) {
                    _log.error("Файл не удовлетворяет фильтрации <%s>. Контент не поддерживается.", currentFileInfo.getAbsolutePath());
                } else {
                    setFiles(processedFiles);
                    this.downloadDto = null;
                    downloadButton.setDisable(false);
                    _log.info("Чтение успешно для файла <%s>", currentFileInfo.getFilename());
                }
                showErrorsButton.setVisible(true);
            }
            startButton.setDisable(false);
        }));

        FileChooser chooserForSave = new FileChooser();
        chooserForSave.setTitle("Сохранение результата");
        downloadButton.setDisable(true);
        downloadButton.setOnAction(e -> Platform.runLater(() -> {
            if (processedFiles != null && !CollectionUtils.isEmpty(processedFiles.getProcessedFiles())) {
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
                        FileUtils.copyInputStreamToFile(downloadDto.getContent(), saveFile);
                    } catch (IOException ex) {
                        _log.error("Не удалось сохранить файл <%s>. Ошибка: %s", saveFile.getName(), ex.getMessage());
                    }
                    _log.info("Файл успешно преобразован и сохранён <%s>", saveFile.getName());
                    if (!CollectionUtils.isEmpty(downloadDto.getNotFound())) {
                        for (Map.Entry<String, List<AntibioticGram.AntibioticoGramItem>> entry : downloadDto.getNotFound().entrySet()) {
                            if (!CollectionUtils.isEmpty(entry.getValue())) {
                                _log.error("Не удалось найти колонки для <%s>: %s", entry.getKey(), StringUtils.join(entry.getValue().stream().map(s -> s.name).collect(Collectors.toSet()), ","));
                            }
                        }
                    }
                }
            } else {
                _log.error("Д");
                downloadButton.setDisable(true);
            }
        }));
    }

    private void setFiles(ProcessResponse<MedicalDocFile> res) {
        this.fileList.getItems().clear();
        List<MedicalDocFile> result = Stream.concat(res.getProcessedFiles().stream(), res.getErrorFiles().stream())
                .sorted(Comparator.comparing(MedicalDocFile::getFilename))
                .toList();
        this.fileList.getItems().addAll(result);
        countNameLabel.setVisible(true);
        countLabel.setText(String.valueOf(this.fileList.getItems().size()));
    }

    private void logDelim() {
        if (!logView.getItems().isEmpty()) {
            _log.info("=======================================================================");
        }
    }

    private static void setLogView(ListView<String> logView) {
        logView.getStylesheets().clear();
        if (CURRENT_THEME == LIGHT) {
            String styleSheet = "/ru/work/service/view/css/log-list-light.css";
            logView.getStylesheets().add(styleSheet);
        } else {
            String styleSheet = "/ru/work/service/view/css/log-list-dark.css";
            logView.getStylesheets().add(styleSheet);
        }
    }

}
