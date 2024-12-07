package ru.work.service.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.util.CollectionUtils;
import ru.work.service.dto.enums.ProcessedStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Setter
@Getter
@NoArgsConstructor
public class ProcessResponse<T extends FileDto> {

    private Integer countForProcess = 0;
    private List<T> processedFiles = new ArrayList<>();
    private List<T> errorFiles = new ArrayList<>();

    public ProcessResponse(List<T> files) {
        Map<Status, List<T>> filesByStatus = files.stream()
                .collect(Collectors.groupingBy(f -> {
                    if (f.getStatus() == ProcessedStatus.SUCCESS) {
                        this.countForProcess = this.countForProcess + f.getCount();
                        return Status.SUCCESS;
                    } else {
                        return Status.ERROR;
                    }
                }));
        this.processedFiles = filesByStatus.get(Status.SUCCESS);
        this.errorFiles = filesByStatus.get(Status.ERROR);
    }

    public enum Status {
        SUCCESS,
        ERROR
    }

    public List<T> getProcessedFiles() {
        return CollectionUtils.isEmpty(processedFiles) ? new ArrayList<>() : processedFiles;
    }

    public List<T> getErrorFiles() {
        return CollectionUtils.isEmpty(errorFiles) ? new ArrayList<>() : errorFiles;
    }
}
