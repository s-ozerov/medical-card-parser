package ru.work.service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Setter
@Getter
@ConfigurationProperties(prefix = "medical-template")
public class MedicalTemplateProperties {

    private String key;
    private Map<String, List<String>> columns = new TreeMap<>();
    private ColumnEnabled columnEnabled;

    @Setter
    @Getter
    public static class ColumnEnabled {
        private Boolean filename;
    }
}
