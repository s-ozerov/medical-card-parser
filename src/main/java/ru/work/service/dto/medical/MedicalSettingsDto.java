package ru.work.service.dto.medical;

import lombok.Builder;
import lombok.Data;
import ru.work.service.dto.SheetSettings;
import ru.work.service.view.util.Theme;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Data
@Builder
public class MedicalSettingsDto implements SheetSettings, Serializable {

    @Serial
    private static final long serialVersionUID = 919884135553052457L;

    private String lastFilePatch;
    private String lastFolderPatch;
    private Theme currentTheme;

    private ColumnEnabledSettings columnEnabled;
    private Map<String, List<String>> columns = new TreeMap<>();

    @Data
    @Builder
    public static class ColumnEnabledSettings implements Serializable {

        @Serial
        private static final long serialVersionUID = 467539522311682594L;

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
