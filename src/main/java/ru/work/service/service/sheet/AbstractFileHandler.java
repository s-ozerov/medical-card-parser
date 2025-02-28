package ru.work.service.service.sheet;

import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import ru.work.service.dto.DownloadDto;
import ru.work.service.dto.FileDto;
import ru.work.service.dto.ProcessResponse;
import ru.work.service.helper.FileHelper;
import ru.work.service.service.doc.DocTemplate;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;

public abstract class AbstractFileHandler<T extends FileDto> {

    private final DocTemplate<T> docTemplate;

    public AbstractFileHandler(DocTemplate<T> template) {
        this.docTemplate = template;
    }

    public ProcessResponse<T> readFile(FileDto file) {
        return docTemplate.read(Collections.singletonList(file), null, null);
    }

    public ProcessResponse<T> readFiles(String path, ProgressBar progressBar, Label loadingText) {
        List<FileDto> files = FileHelper.getFilesDOCorDOCXByPatch(path);
        if (progressBar == null) {
            progressBar = new ProgressBar(files.size());
        }
        return docTemplate.read(files, progressBar, loadingText);
    }

    protected abstract DownloadDto download(String downloadFilename, ByteArrayOutputStream content);
}
