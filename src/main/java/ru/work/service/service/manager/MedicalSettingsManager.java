package ru.work.service.service.manager;

import lombok.extern.slf4j.Slf4j;
import ru.work.service.dto.medical.MedicalSettingsDto;
import ru.work.service.exception.FailedSaveSettingsException;
import ru.work.service.view.util.Theme;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
public class MedicalSettingsManager {

    private static final String FILE_NAME_SETTINGS = "settings";
    private static final String PATH_SETTINGS = System.getProperty("user.dir") + "\\" + FILE_NAME_SETTINGS;

    public static MedicalSettingsDto saveSettings(MedicalSettingsDto settings) {
        return write(settings);
    }

    public static MedicalSettingsDto readSettings() {
        MedicalSettingsDto settings = read();
        if (settings == null) {
            return defaultSettings();
        }
        return settings;
    }

    private static MedicalSettingsDto write(MedicalSettingsDto settings) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(PATH_SETTINGS))) {
            oos.writeObject(settings);
            log.info("Success save settings");
            return settings;
        } catch (IOException e) {
            log.error("Failed write in file: {}", e.getMessage());
            throw new FailedSaveSettingsException("Не удалось сохранить настройки по пути: %s".formatted(PATH_SETTINGS));
        }
    }

    private static MedicalSettingsDto read() {
        MedicalSettingsDto settings = null;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(PATH_SETTINGS))) {
            settings = (MedicalSettingsDto) ois.readObject();
            log.info("Success read settings");
        } catch (IOException | ClassNotFoundException e) {
            log.error("Failed read from file: {}", e.getMessage());
        }
        return settings;
    }


    private static MedicalSettingsDto defaultSettings() {
        Map<String, LinkedList<String>> columns = new TreeMap<>();
        columns.put("AMP", new LinkedList<>(List.of("Ампициллин")));
        columns.put("AZIT", new LinkedList<>(List.of("Азитромицинк")));
        columns.put("AMP_SUL", new LinkedList<>(List.of("Ампициллин/сульбактам")));
        columns.put("BEN", new LinkedList<>(List.of("Бензилпенициллин")));
        columns.put("DAP", new LinkedList<>(List.of("Даптомицин")));
        columns.put("OXC", new LinkedList<>(List.of("Оксациллин")));
        columns.put("FAZ", new LinkedList<>(List.of("Цефазолин")));
        columns.put("FOT", new LinkedList<>(List.of("Цефотаксим")));
        columns.put("FTA", new LinkedList<>(List.of("Цефтазидим")));
        columns.put("FTR", new LinkedList<>(List.of("Цефтриаксон")));
        columns.put("FEP", new LinkedList<>(List.of("Цефепим")));
        columns.put("AMO_KLA", new LinkedList<>(List.of("Амоксициллин/клавуланат")));
        columns.put("PIP_TAZ", new LinkedList<>(List.of("Пиперациллин/тазобактам")));
        columns.put("IMI", new LinkedList<>(List.of("Имипенем")));
        columns.put("MER", new LinkedList<>(List.of("Меропенем")));
        columns.put("ERT", new LinkedList<>(List.of("Эртапенем")));
        columns.put("GEN", new LinkedList<>(List.of("Гентамицин")));
        columns.put("AMI", new LinkedList<>(List.of("Амикацин")));
        columns.put("LEV", new LinkedList<>(List.of("Левофлоксацин")));
        columns.put("CIP", new LinkedList<>(List.of("Ципрофлоксацин")));
        columns.put("ERI", new LinkedList<>(List.of("Эритромицин")));
        columns.put("AZI", new LinkedList<>(List.of("Азитромицин")));
        columns.put("KLA", new LinkedList<>(List.of("Кларитромицин")));
        columns.put("VAN", new LinkedList<>(List.of("Ванкомицин")));
        columns.put("TIG", new LinkedList<>(List.of("Тигециклин")));
        columns.put("LIN", new LinkedList<>(List.of("Линезолид")));
        columns.put("TRI_SUL", new LinkedList<>(List.of("Триметоприм/сульфаметоксазол", "Триметоприм-сульфаметоксазол")));
        columns.put("TRI", new LinkedList<>(List.of("Триметоприм")));
        columns.put("POL", new LinkedList<>(List.of("Полимиксин")));
        columns.put("FOSF", new LinkedList<>(List.of("Фосфомицин")));
        columns.put("NITRO", new LinkedList<>(List.of("Нитрофурантоин")));
        columns.put("TETR", new LinkedList<>(List.of("Тетрациклин")));
        columns.put("RIF", new LinkedList<>(List.of("Рифампицин")));
        columns.put("FLU", new LinkedList<>(List.of("Флуконазод", "Флуконазол")));
        columns.put("CAS", new LinkedList<>(List.of("Каспофунгин")));
        columns.put("VAR", new LinkedList<>(List.of("Вориконазол")));
        columns.put("COLI1", new LinkedList<>(List.of("Колистин,К ≤ 1мг/л")));
        columns.put("COL1", new LinkedList<>(List.of("Колистин МПК ≤ 1мг/л")));
        columns.put("COL2", new LinkedList<>(List.of("Колистин МПК ≤ 2мг/л", "Колистин МПК 2 мг/л")));
        columns.put("COL4", new LinkedList<>(List.of("Колистин МПК ≤ 4мг/л", "Колистин МПК 4 мг/л")));
        columns.put("COLX2", new LinkedList<>(List.of("Колистин МПК > 2 мг/л")));
        columns.put("FTOR", new LinkedList<>(List.of("Фторхинолоны")));
        columns.put("AMIN", new LinkedList<>(List.of("Аминогликозиды")));
        columns.put("KARB", new LinkedList<>(List.of("β-лактамы (карбапенемы)")));
        columns.put("MACRO", new LinkedList<>(List.of("Макролиды")));
        columns.put("CEF", new LinkedList<>(List.of("Цефокситин")));

        return MedicalSettingsDto.builder()
                .lastPatch("user.home")
                .currentTheme(Theme.DARK)
                .columnEnabled(MedicalSettingsDto.ColumnEnabledSettings.builder()
                        .month(true)
                        .ib(false)
                        .bioMaterial(true)
                        .diagnose(false)
                        .division(true)
                        .patient(false)
                        .numberAnalyze(false)
                        .receiveMaterialDate(false)
                        .filename(false)
                        .build())
                .columns(columns)
                .build();
    }

}
