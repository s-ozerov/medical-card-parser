package ru.work.service.service.doc;

import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import ru.work.service.dto.FileDto;
import ru.work.service.dto.ProcessResponse;

import java.util.List;

public interface DocTemplate<T extends FileDto> {

    ProcessResponse<T> read(List<FileDto> files, ProgressBar progressBar, Label loadingText);

}
