package ru.work.service.dto.medical;

import lombok.Builder;
import lombok.Data;
import ru.work.service.dto.SheetSettings;
import ru.work.service.view.util.Theme;

import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

@Data
@Builder
public class MedicalSettingsDto implements SheetSettings {

    private String lastPatch;
    private Theme currentTheme;

    private ColumnEnabledSettings columnEnabled;
    private Map<String, LinkedList<String>> columns = new TreeMap<>();

    @Data
    @Builder
    public static class ColumnEnabledSettings {
        private boolean month;
        private boolean microorganisms;
        private boolean division;
        private boolean bioMaterial;
        private boolean receiveMaterialDate;
        private boolean filename;
        private boolean patient;
        private boolean diagnose;
        private boolean ib;
        private boolean numberAnalyze;

    }


}
