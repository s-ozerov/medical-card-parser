package ru.work.service.dto.medical;

import lombok.Builder;
import lombok.Data;
import ru.work.service.view.util.Theme;

import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

@Data
@Builder
public class MedicalSettingsDto {

    private String lastPatch;
    private Theme currentTheme;

    private ColumnEnabledSettings columnEnabled;
    private Map<String, LinkedList<String>> columns = new TreeMap<>();

    @Data
    @Builder
    public static class ColumnEnabledSettings {
        private boolean filename;
        private boolean receiveMaterialDate;
        private boolean patient;
        private boolean bioMaterial;
        private boolean diagnose;
        private boolean ib;
        private boolean numberAnalyze;
        private boolean division;
    }


}
