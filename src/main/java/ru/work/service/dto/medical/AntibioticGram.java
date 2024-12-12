package ru.work.service.dto.medical;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
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

        public AntibioticoGramItem(String name, List<String> result) {
            this.name = name;
            this.result = new LinkedHashMap<>();
            this.size = 0;

            for (int i = 0; i < result.size(); i++) {
                if (StringUtils.isNotBlank(result.get(i))) {
                    this.result.put(i + 1, result.get(i));
                    this.size++;
                }
            }
        }
    }
}
