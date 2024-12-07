package ru.work.service.dto;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import ru.work.service.dto.enums.Extension;
import ru.work.service.dto.enums.ProcessedStatus;

import java.io.File;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

import static ru.work.service.helper.FileHelper.calculateSize;

@Setter
@Getter
public class FileDto implements Serializable {

    @Serial
    private static final long serialVersionUID = -8342572077381456212L;

    private String filename;
    private String absolutePath;
    private Extension extension;
    private BigDecimal sizeKb;

    private ProcessedStatus status;
    private String errorMessage;

    public FileDto(File file) {
        this.filename = file.getName();
        this.absolutePath = file.getAbsolutePath();
        this.extension = Extension.fromFile(this.filename);
        this.sizeKb = calculateSize(file.length());
        this.status = ProcessedStatus.COMPLETE_READ;
    }

    protected FileDto(FileDto file) {
        this.filename = file.getFilename();
        this.absolutePath = file.getAbsolutePath();
        this.extension = file.getExtension();
        this.sizeKb = file.getSizeKb();
        this.status = file.getStatus();
        this.errorMessage = file.getErrorMessage();
    }

    public FileDto(ProcessedStatus status, String errorMessage) {
        this.status = status;
        this.errorMessage = errorMessage;
    }

    public void setFailed(ProcessedStatus status, String errorMessage) {
        if (StringUtils.isBlank(errorMessage)) {
            errorMessage = status.getMessage();
        }
        this.status = status;
        this.errorMessage = errorMessage;
    }

    public Integer getCount() {
        return 0;
    }

    @Override
    public String toString() {
        String extension = this.extension == null ? "" : this.extension.name();
        return """
                %s - размер: %s КБ, путь: %s, формат: %s
                """.formatted(filename, sizeKb.toString(), StringUtils.remove(absolutePath, filename), extension);
    }
}
