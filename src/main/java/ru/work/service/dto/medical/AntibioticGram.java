package ru.work.service.dto.medical;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PUBLIC)
public class AntibioticGram {

    String header;
    LinkedList<AntibioticoGramItem> items;

    @FieldDefaults(level = AccessLevel.PUBLIC)
    public static class AntibioticoGramItem {
        String name;
        Integer size;
        Map<Integer, String> result;

        public AntibioticoGramItem(String name, String[] result) {
            this.name = name;
            this.result = new LinkedHashMap<>();
            this.size = 0;

            for (int i = 0; i < result.length; i++) {
                if (StringUtils.isNotBlank(result[i])) {
                    this.result.put(i + 1, result[i]);
                    this.size++;
                }
            }
        }
    }
}
