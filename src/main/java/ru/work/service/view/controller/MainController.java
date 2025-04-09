package ru.work.service.view.controller;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import com.github.plushaze.traynotification.notification.Notifications;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxmlView;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import ru.work.service.anotations.Css;
import ru.work.service.dto.DownloadDto;
import ru.work.service.dto.FileDto;
import ru.work.service.dto.ProcessResponse;
import ru.work.service.dto.enums.Extension;
import ru.work.service.dto.enums.ProcessedStatus;
import ru.work.service.dto.medical.MedicalDocFile;
import ru.work.service.dto.medical.MedicalSettingsDto;
import ru.work.service.helper.FileHelper;
import ru.work.service.service.manager.MedicalSettingsManager;
import ru.work.service.service.sheet.MedicalFileHandler;
import ru.work.service.view.JavaFxApplication;
import ru.work.service.view.component.DownloadComponent;
import ru.work.service.view.component.ExceptionBox;
import ru.work.service.view.component.FileBox;
import ru.work.service.view.component.modal.NestedListComponent;
import ru.work.service.view.component.treeview.TreeViewComponent;
import ru.work.service.view.component.treeview.TreeViewItem;
import ru.work.service.view.factory.LogFactory;
import ru.work.service.view.util.ControllerUtil;
import ru.work.service.view.util.ImageFactory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static ru.work.service.dto.enums.Extension.DOC;
import static ru.work.service.dto.enums.Extension.DOCX;
import static ru.work.service.view.component.notification.NotificationUtil.showAlert;
import static ru.work.service.view.util.Constants.CURRENT_THEME;
import static ru.work.service.view.util.ImageFactory.buildSize36;
import static ru.work.service.view.util.ImageFactory.buildSize48;
import static ru.work.service.view.util.StyleFactory.setTheme;
import static ru.work.service.view.util.Theme.DARK;
import static ru.work.service.view.util.Theme.LIGHT;

@Slf4j
@Component
@RequiredArgsConstructor
@FxmlView("main.fxml")
public class MainController {

    private MainController mainController;

    @Autowired
    public void setSelf(MainController self) {
        this.mainController = self;
    }

    @FXML
    @Css(dark = "log-list-dark.css", light = "log-list-light.css")
    private ListView<String> logView;

    @FXML
    @Css(dark = "file-list-dark.css", light = "file-list-light.css")
    private ListView<MedicalDocFile> fileList;

    @FXML
    @Css(dark = "button-super-dark.css", light = "button-super-light.css")
    private Button startButton;

    @FXML
    @Css(dark = "button-icon-dark.css", light = "button-icon-light.css")
    private Button openButton;

    @FXML
    @Css(dark = "button-icon-dark.css", light = "button-icon-light.css")
    private Button openMultipleButton;

    @FXML
    @Css(dark = "button-super-dark.css", light = "button-super-light.css")
    private Button saveSettingsButton;

    @FXML
    private Button clearLogButton;
    @FXML
    private Button downloadButton;
    @FXML
    private Button changeAntibioticiButton;

    @FXML
    private Label countLabel;
    @FXML
    private Label countSuccessFilesLabel;
    @FXML
    private Label countSuccessForInputLabel;
    @FXML
    private Label countSuccessBioLabel;
    @FXML
    private Label countErrorFilesLabel;

    @FXML
    private ChoiceBox<String> themeBox;
    @FXML
    private VBox errorHandlerBox;
    @FXML
    private VBox errorBox;
    @FXML
    private AnchorPane manualPane;

    @FXML
    private CheckBox monthCB;
    @FXML
    private CheckBox microorganismsCB;
    @FXML
    private CheckBox divisionCB;
    @FXML
    private CheckBox bioMaterialCB;
    @FXML
    private CheckBox receiveMaterialDateCB;
    @FXML
    private CheckBox filenameCB;
    @FXML
    private CheckBox patientCB;
    @FXML
    private CheckBox diagnoseCB;
    @FXML
    private CheckBox ibCB;
    @FXML
    private CheckBox numberAnalyzeCB;

    private final MedicalFileHandler medicalHandler;
    private final DownloadComponent downloadComponent;

    private LogFactory _log;
    private FileDto currentFileInfo;
    private String currentPath;

    private ProcessResponse<MedicalDocFile> processedFiles;
    private DownloadDto downloadDto;

    private Map<String, List<String>> items = null;

    @FXML
    public void initialize() {
        initSettings();

        setThemeForButtons();
        setTheme(mainController);

        errorBox.setVisible(false);

        _log = new LogFactory(this.getClass(), logView);

        initButtons();
        initListView();

        TreeViewComponent manualComponent = getManualComponent();
        manualPane.getChildren().add(manualComponent.create());

        themeBox.setValue(CURRENT_THEME.name());
        themeBox.setItems(FXCollections.observableArrayList(DARK.name(), LIGHT.name()));
        themeBox.setOnAction(clickEvent -> {
            if (themeBox.getValue().equalsIgnoreCase(DARK.name())) {
                JavaFxApplication.setTheme(new PrimerDark());
                CURRENT_THEME = DARK;
            } else {
                JavaFxApplication.setTheme(new PrimerLight());
                CURRENT_THEME = LIGHT;
            }
            log.info("Current theme: {}", CURRENT_THEME);
            setTheme(mainController);
        });
        themeBox.getStylesheets().add("/ru/work/service/view/css/choice-box.css");
    }

    private void initSettings() {
        MedicalSettingsDto settingsDto = MedicalSettingsManager.readSettings();

        CURRENT_THEME = settingsDto.getCurrentTheme();

        MedicalSettingsDto.ColumnEnabledSettings enabledSettings = settingsDto.getColumnEnabled();
        monthCB.setSelected(enabledSettings.isMonth());
        microorganismsCB.setSelected(enabledSettings.isMicroorganisms());
        divisionCB.setSelected(enabledSettings.isDivision());
        bioMaterialCB.setSelected(enabledSettings.isBioMaterial());
        receiveMaterialDateCB.setSelected(enabledSettings.isReceiveMaterialDate());
        filenameCB.setSelected(enabledSettings.isFilename());
        patientCB.setSelected(enabledSettings.isPatient());
        diagnoseCB.setSelected(enabledSettings.isDiagnose());
        ibCB.setSelected(enabledSettings.isIb());
        numberAnalyzeCB.setSelected(enabledSettings.isNumberAnalyze());
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
        saveSettingsButton.getStyleClass().add("super-button");

        saveSettingsButton.setOnAction(e -> {
            MedicalSettingsDto settingsDto = MedicalSettingsManager.readSettings();
            MedicalSettingsDto.ColumnEnabledSettings enabledSettings = MedicalSettingsDto.ColumnEnabledSettings
                    .builder()
                    .month(monthCB.isSelected())
                    .microorganisms(microorganismsCB.isSelected())
                    .ib(ibCB.isSelected())
                    .bioMaterial(bioMaterialCB.isSelected())
                    .diagnose(diagnoseCB.isSelected())
                    .division(divisionCB.isSelected())
                    .patient(patientCB.isSelected())
                    .numberAnalyze(numberAnalyzeCB.isSelected())
                    .receiveMaterialDate(receiveMaterialDateCB.isSelected())
                    .filename(filenameCB.isSelected())
                    .build();
            settingsDto.setColumnEnabled(enabledSettings);
            settingsDto.setCurrentTheme(CURRENT_THEME);

            MedicalSettingsManager.saveSettings(settingsDto);
            log.info("Settings saved");
        });

        changeAntibioticiButton.setOnAction(e -> {
            NestedListComponent nestedListComponent = new NestedListComponent();
            MedicalSettingsDto settingsDto = MedicalSettingsManager.readSettings();

            nestedListComponent.display("Настройка антибиотикограммы", settingsDto.getColumns());
            items = nestedListComponent.getItems();
            if (items != null) {
                settingsDto.setColumns(items);
            }
            MedicalSettingsManager.saveSettings(settingsDto);
        });

        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выбор файла");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(DOC.name(), DOC.filter),
                new FileChooser.ExtensionFilter(DOCX.name(), DOCX.filter),
                new FileChooser.ExtensionFilter("Все файлы", "*.*")
        );

        startButton.setDisable(true);

        openButton.setOnAction(e -> {
            MedicalSettingsDto settingsDto = MedicalSettingsManager.readSettings();
            fileChooser.setInitialDirectory(new File(settingsDto.getLastFilePatch()));

            File file = fileChooser.showOpenDialog(JavaFxApplication.WINDOW);
            if (file != null) {
                String filename = file.getName();
                String path = file.getAbsolutePath();

                logDelim();

                _log.info("Путь к файлу «%s»", path);
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
                        errorBox.setVisible(false);

                        startButton.setDisable(false);
                        startButton.setText("Получить из файла");
                        this.currentFileInfo = currentFileInfo;

                        settingsDto.setLastFilePatch(path.substring(0, path.lastIndexOf('\\')));
                        MedicalSettingsManager.saveSettings(settingsDto);
                    }
                }
            }
        });

        final DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Выбор папки с файлами");

        openMultipleButton.setOnAction(e -> {
            MedicalSettingsDto settingsDto = MedicalSettingsManager.readSettings();
            directoryChooser.setInitialDirectory(new File(settingsDto.getLastFolderPatch()));

            final File selectedDirectory = directoryChooser.showDialog(JavaFxApplication.WINDOW);
            if (selectedDirectory != null) {
                this.currentFileInfo = null;
                this.fileList.getItems().clear();
                countLabel.setText("");
                errorBox.setVisible(false);

                logDelim();

                _log.info("Найдена папка: %s", selectedDirectory.getAbsolutePath());
                this.currentPath = selectedDirectory.getPath();

                var path = selectedDirectory.getPath();
                settingsDto.setLastFolderPatch(path);
                var savePath = path.substring(0, path.lastIndexOf('\\'));
                settingsDto.setLastFolderPatch(savePath);
                MedicalSettingsManager.saveSettings(settingsDto);

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
            errorBox.setVisible(false);
            errorHandlerBox.getChildren().clear();
            countErrorFilesLabel.setText("0");

            fileList.getItems().clear();
            if (isNull(currentFileInfo)) {
                if (StringUtils.isBlank(currentPath)) {
                    ExceptionBox.displayWarn("Ошибка", "Ошибка запуска операции. Файл или папка не выбраны");
                    _log.error("Ошибка запуска операции. Файл или папка не выбраны");
                } else {
                    _log.info("Поиск файлов на чтение по пути «%s»", currentPath);
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
                _log.info("Поиск на чтение файла «%s» ", currentFileInfo.getFilename());

                this.processedFiles = medicalHandler.readFile(currentFileInfo);
                if (CollectionUtils.isEmpty(processedFiles.getSuccessFiles())) {
                    _log.error("Файл не удовлетворяет фильтрации «%s». Контент не поддерживается.", currentFileInfo.getAbsolutePath());
                    FileBox.displayFilesInfo(processedFiles,
                            countSuccessFilesLabel, countSuccessForInputLabel, countSuccessBioLabel,
                            errorBox, errorHandlerBox, countErrorFilesLabel
                    );
                } else {
                    setFiles(processedFiles);
                    this.downloadDto = null;
                    downloadButton.setDisable(false);

                    FileBox.displayFilesInfo(processedFiles,
                            countSuccessFilesLabel, countSuccessForInputLabel, countSuccessBioLabel,
                            errorBox, errorHandlerBox, countErrorFilesLabel
                    );

                    _log.info("Чтение успешно для файла «%s»", currentFileInfo.getFilename());
                }
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
                            _log.info("Файл повторно сохранён «%s»", saveFile.getName());
                            showAlert("Загрузка завершена", "Файл успешно сохранён:\n«%s»".formatted(downloadFilename), Notifications.INFORMATION);
                        } catch (IOException ex) {
                            _log.error("Не удалось сохранить файл «%s». Ошибка: %s", saveFile.getName(), ex.getMessage());
                        }
                        return;
                    }
                    _log.info("Преобразуем содержимое файла/файлов в XLSX формат по пути «%s»", saveFile.getAbsolutePath());
                    DownloadDto downloadDto = medicalHandler.convertDOCToXLSX(downloadFilename, processedFiles);
                    try {
                        if (downloadDto == null) {
                            _log.error("Не удалось сохранить файл «%s». Ошибка: %s", saveFile.getName(), "выбранный doc файл не обработан");
                        } else {
                            FileUtils.copyInputStreamToFile(downloadDto.getContent(), saveFile);
                            _log.info("Успешно преобразован и сохранён.");
                            _log.info("Файл: %s", saveFile.getName());
                            showAlert("Загрузка завершена", "Файл успешно сохранён:\n«%s»".formatted(saveFile.getName()), Notifications.INFORMATION);
                            if (!CollectionUtils.isEmpty(downloadDto.getNotFound())) {
                                for (Map.Entry<String, String> entry : downloadDto.getNotFound().entrySet()) {
                                    if (!StringUtils.isNotBlank(entry.getValue())) {
                                        _log.error("Не найдены колонки «%s»: %s", entry.getKey(), entry.getValue());
                                    }
                                }
                            }
                        }
                    } catch (IOException ex) {
                        _log.error("Не удалось сохранить файл «%s». Ошибка: %s", saveFile.getName(), ex.getMessage());
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

        FileBox.displayFilesInfo(processedFiles,
                countSuccessFilesLabel, countSuccessForInputLabel, countSuccessBioLabel,
                errorBox, errorHandlerBox, countErrorFilesLabel
        );
    }

    private void processReadFilesSuccess() {
        log.info("Success processed files: {}", currentPath);
        if (processedFiles == null || CollectionUtils.isEmpty(processedFiles.getSuccessFiles())) {
            _log.error("Не найдены файлы удовлетворяющие фильтры «%s»", currentPath);
        } else {
            setFiles(processedFiles);
            downloadDto = null;
            downloadButton.setDisable(false);
            _log.info("Чтение успешно по пути «%s»", currentPath);
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
        errorBox.setVisible(true);
    }

    private void setFiles(ProcessResponse<MedicalDocFile> res) {
        List<MedicalDocFile> result = Stream.concat(res.getSuccessFiles().stream(), res.getErrorFiles().stream())
                .sorted(Comparator.comparing(MedicalDocFile::getFilename))
                .toList();
        fileList.getItems().addAll(result);
        countLabel.setText(String.valueOf(fileList.getItems().size()));
    }

    private void logDelim() {
        if (!logView.getItems().isEmpty()) {
            _log.info("=======================================================================");
        }
    }

    private void setThemeForButtons() {
        downloadButton.setGraphic(buildSize36(ImageFactory.ImageName.DOWNLOAD));
        String styleSheet = "/ru/work/service/view/css/button-download.css";
        downloadButton.getStylesheets().add(styleSheet);

        openButton.setGraphic(buildSize36(ImageFactory.ImageName.WORD));
        openMultipleButton.setGraphic(buildSize36(ImageFactory.ImageName.FOLDER));
        clearLogButton.setGraphic(buildSize48(ImageFactory.ImageName.DELETE));
        clearLogButton.setStyle("""
                -fx-background-color: transparent;
                -fx-cursor: hand;
                """);
    }

    private static TreeViewComponent getManualComponent() {
        var manualItems = List.of(
                new TreeViewItem("1. Сводка по программе", List.of(
                        new TreeViewItem.SubItem("Что это такое?", """
                                Утилита разработана для обработки данных о пациентах.
                                
                                Данная программа-парсер выполняет функцию преобразования данных из одного файла или множества файлов расширения DOC или DOCX
                                в один файл excel, содержащий итоговую выдержку по всем  обработанным файлам.
                                
                                С помощью данной утилиты возможна настройка отображаемых данных в результирующем файле.
                                """))),
                new TreeViewItem("2. Основные функции работы с файлами", List.of(
                        new TreeViewItem.SubItem("Кнопка - «Выбрать 1 файл DOC или DOCX»", """
                                Эта кнопка позволяет выбрать 1 файл расширения «*.doc» или «*.docx» в системе для последующей обработки.
                                Необходимо учитывать, что содержание файла должно соответствовать шаблону для корректного считывания данных.
                                По умолчанию установлен фильтр «*.doc» для поиска подходящих файлов. Его можно переключить.
                                """),
                        new TreeViewItem.SubItem("Кнопка - «Выбрать несколько файлов DOC или DOCX»", """
                                Эта кнопка позволяет выбрать множество файлов расширения «*.doc» и «*.docx» в указанной папке для последующей обработки.
                                Учитываются все файлы, указанных выше форматов. Данная кнопка значительно ускоряет процесс обработки большого количества файлов.
                                
                                Для успешного завершения обработки выбранной папки следует указать папку,
                                в которой находятся только вложенные папки и соответствующие шаблону файлы.
                                """),
                        new TreeViewItem.SubItem("Кнопка - «Обработать файл/файлы по указанному пути»", """
                                Данная кнопка запускает процесс считывания файла или множества вложенных файлов в директории и
                                последующего преобразования информации в объекты.
                                
                                Повторный запуск по тем же файлам работает быстрее за счёт кэширования данных в утилите.
                                
                                По завершению обработки (когда скроется окно процесса загрузки) все считанные файлы
                                с расширением «*.doc» и «*.docx» отобразятся на панели слева, в списке файлов. Двойным нажатием по
                                любому объекту в списке отобразится считанная информация по выбранному файлу и краткая сводка
                                по успешной/не успешной обработке.
                                
                                По процессу обработки также отобразятся записи выполненных этапов во вкладке «Журнал», а общий результат во вкладке «Результат».
                                """),
                        new TreeViewItem.SubItem("Кнопка - «Сохранить результат в файл Excel»", """
                                Данная кнопка позволяет преобразовать считанные объекты из файлов с расширением «*.doc» и «*.docx» в таблицу Excel.
                                Входными данными будут все файлы которые находятся в списке на панели слева.
                                
                                При каждом нажатии на кнопку подгружаются установленные настройки из вкладки «Настройки».
                                
                                Имя файла генерируется автоматически вида «результат 09-04-2025 22-51-57-968.xlsx». Его можно переименовать, но расширение следует оставить «*.xlsx».
                                """))
                ),
                new TreeViewItem("3. Вкладки", List.of(
                        new TreeViewItem.SubItem("Журнал", """
                                В данном разделе выводится основные действия выполняемые программой.
                                
                                Здесь можно отслеживать ошибки и результат обработки файлов.
                                """),
                        new TreeViewItem.SubItem("Результат", """
                                В данном разделе находится сводка по обработанным файлам.
                                
                                Ошибки в обработке - это не всегда критические ошибки.
                                Чаще всего это файлы которые не подходят под формирование записи в результирующий файл. Например, в файле нет антибиотикограммы
                                или в файле не заполнена таблица с микроорганизмами.
                                """),
                        new TreeViewItem.SubItem("Настройки", """
                                Раздел для управления всеми настройками приложения.
                                
                                Здесь можно выбрать поля, которые попадут как столбцы в итоговую таблицу.
                                Кнопка «Сохранить изменения» отвечает за сохранения этих полей и темы.
                                
                                Антибиотикограмма сохраняется отдельно при каждом закрытии окна настроек антибиотикограммы.
                                
                                Также, самое основное, здесь можно изменить отображение столбцов антибиотикограммы в результирующем файле excel.
                                Во время сохранения результата обработки в директорию, элементы из верхнего списка (по умолчанию AMI, AMIN, AMO_KLA и т.д.)
                                используются как аббревиатура для наименования столбцов, а элементы нижнего списка как соответствующее ему значение антибиотика из DOC(X) файла.
                                
                                Пример:
                                В антибиотикограмме у пациента есть строчка:
                                |Ампициллин | - | R |
                                При формировании записи в итоговой таблице столбец определяется по соответствию:
                                AMP (столбец) - Ампициллин (значение)
                                
                                Каждому столбцу можно присваивать несколько значений (из-за встречающихся опечаток):
                                COL4 - { Колистин МПК <= 4мг/л; Колистин МПК 4мг/л }
                                FLU  - { Флуконазод; Флуконазол }
                                """)
                ))
        );
        return new TreeViewComponent("Информация по работе с программой", manualItems);
    }

}
