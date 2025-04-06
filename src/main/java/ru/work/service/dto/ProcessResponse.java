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
    private List<T> successFiles;
    private List<T> errorFiles;

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
        this.successFiles = filesByStatus.get(Status.SUCCESS);
        this.errorFiles = filesByStatus.get(Status.ERROR);
        this.successFiles = CollectionUtils.isEmpty(this.successFiles) ? new ArrayList<>() : this.successFiles;
        this.errorFiles = CollectionUtils.isEmpty(this.errorFiles) ? new ArrayList<>() : this.errorFiles;
    }

    public enum Status {
        SUCCESS,
        ERROR
    }

}
