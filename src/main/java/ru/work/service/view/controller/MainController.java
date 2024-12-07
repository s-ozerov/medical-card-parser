package ru.work.service.view.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
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
import ru.work.service.dto.medical.MedicalDocFile;
import ru.work.service.helper.FileHelper;
import ru.work.service.service.sheet.MedicalSheetFileHandler;
import ru.work.service.view.JavaFxApplication;
import ru.work.service.view.component.ExceptionBox;
import ru.work.service.view.component.FileBox;
import ru.work.service.view.factory.LogFactory;
import ru.work.service.view.util.ControllerUtil;
import ru.work.service.view.util.StageUtil;
import ru.work.service.view.util.Theme;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static ru.work.service.dto.enums.Extension.DOC;
import static ru.work.service.dto.enums.Extension.DOCX;
import static ru.work.service.view.util.Constants.CURRENT_THEME;

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
    private Label openTitleLabel;
    @FXML
    private Label openNameLabel;
    @FXML
    private Label countLabel;

    private final MedicalSheetFileHandler medicalHandler;

    private volatile LogFactory _log;
    private FileDto currentFileInfo;
    private String currentPath;

    private ProcessResponse<MedicalDocFile> processedFiles;
    private DownloadDto downloadDto;

    @FXML
    public void initialize() {
        openTitleLabel.setVisible(false);
        openNameLabel.setVisible(false);

        StageUtil.setWidthAndHeight(JavaFxApplication.WINDOW, 950, 590);
        _log = new LogFactory(this.getClass(), logView);

        initButtons();
        initListView();
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
            _log.info("");
            openTitleLabel.setText("");
            openNameLabel.setText("");
            File file = fileChooser.showOpenDialog(JavaFxApplication.WINDOW);
            if (file != null) {
                String filename = file.getName();
                String path = file.getAbsolutePath();

                _log.info("Путь к файлу <%s>", path);
                _log.info("Имя файла: %s", filename);
                openTitleLabel.setText("Выбранный файл:");
                openNameLabel.setText(filename);
                openTitleLabel.setVisible(true);
                openNameLabel.setVisible(true);

                Extension extension = Extension.fromFile(filename);
                String style;
                if (!Extension.isDOC(extension)) {
                    startButton.setDisable(true);
                    downloadButton.setDisable(true);
                    _log.error("Не верный формат файла [%s]", Extension.get(filename));
                    file = null;
                    style = openNameLabel.getStyle();
                    openNameLabel.setStyle(style + "-fx-text-fill: red;");
                } else if (CURRENT_THEME == Theme.WHITE) {
                    startButton.setDisable(false);
                    downloadButton.setDisable(true);
                    style = openNameLabel.getStyle() + "-fx-text-fill: blue;";
                    openNameLabel.setStyle(StringUtils.remove(style, "-fx-text-fill: red;"));
                } else {
                    startButton.setDisable(false);
                    downloadButton.setDisable(true);
                    style = openNameLabel.getStyle();
                    openNameLabel.setStyle(StringUtils.remove(style, "-fx-text-fill: red;"));
                }

                FileDto currentFileInfo = FileHelper.buildFileInfo(file);
                if (currentFileInfo != null) {
                    this.currentPath = null;
                    if (currentFileInfo.getStatus() == ProcessedStatus.FAILED_READ) {
                        ExceptionBox.displayWarn("Ошибка чтения", "Не удалось обработать файл. Ошибка: " + currentFileInfo.getErrorMessage());
                    } else {
                        this.fileList.getItems().clear();
                        countLabel.setText("");
                        showErrorsButton.setVisible(false);

                        startButton.setDisable(false);
                        this.currentFileInfo = currentFileInfo;
                    }
                }
            }
        });

        final DirectoryChooser directoryChooser = new DirectoryChooser();
        openMultipleButton.setOnAction(e -> {
            _log.info("");
            openTitleLabel.setText("");
            openNameLabel.setText("");

            final File selectedDirectory = directoryChooser.showDialog(JavaFxApplication.WINDOW);
            directoryChooser.setTitle("Выбор папки с файлами");
            directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            if (selectedDirectory != null) {
                this.currentFileInfo = null;
                this.fileList.getItems().clear();
                countLabel.setText("");
                showErrorsButton.setVisible(false);
                openTitleLabel.setVisible(true);
                openNameLabel.setVisible(true);

                _log.info("Найдена папка: %s", selectedDirectory.getAbsolutePath());
                openTitleLabel.setText("Выбранная папка:");
                this.currentPath = selectedDirectory.getPath();
                startButton.setDisable(false);
                downloadButton.setDisable(true);
                openNameLabel.setText(selectedDirectory.getPath());
                if (CURRENT_THEME == Theme.WHITE) {
                    openNameLabel.setStyle(openNameLabel.getStyle() + "-fx-text-fill: blue;");
                }
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
                }
            } else {
                _log.error("Д");
                downloadButton.setDisable(true);
            }
        }));
    }

    private void setFiles(ProcessResponse<MedicalDocFile> res) {
        List<MedicalDocFile> result = Stream.concat(res.getProcessedFiles().stream(), res.getErrorFiles().stream())
                .sorted(Comparator.comparing(MedicalDocFile::getFilename))
                .toList();
        this.fileList.getItems().addAll(result);
        countLabel.setText(String.valueOf(this.fileList.getItems().size()));
    }

}
