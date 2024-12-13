package ru.work.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import ru.work.service.dto.medical.AntibioticGram;

import java.io.InputStream;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Setter
@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DownloadDto implements Serializable {

    @Serial
    private static final long serialVersionUID = -1843425079025570268L;

    private String filename;
    private BigDecimal sizeKb;
    private InputStream content;
    private Map<String, List<AntibioticGram.AntibioticoGramItem>> notFound = new HashMap<>();

}
