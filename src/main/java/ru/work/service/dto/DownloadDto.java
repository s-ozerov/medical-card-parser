package ru.work.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.InputStream;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

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

}
