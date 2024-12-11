package ru.work.service.helper;

import lombok.extern.slf4j.Slf4j;
import ru.work.service.dto.FileDto;
import ru.work.service.dto.enums.Extension;
import ru.work.service.dto.enums.ProcessedStatus;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static ru.work.service.helper.MathHelper.MATCH_CONTEXT_18;
import static ru.work.service.helper.MathHelper.round2;

@Slf4j
public class FileHelper {

    public static List<FileDto> getFilesDOCorDOCXByPatch(String path) {
        try (Stream<Path> stream = Files.walk(Paths.get(path))) {
            List<FileDto> files = stream.filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .map(FileHelper::buildFileInfo)
                    .filter(file -> Objects.nonNull(file) && file.getStatus() == ProcessedStatus.COMPLETE_READ &&
                                    (file.getExtension() == Extension.DOC || file.getExtension() == Extension.DOCX))
                    .toList();
            log.info("Find files: {}", files.size());
            return files;
        } catch (IOException e) {
            log.error("Failed to read files in folder <{}> message: {}", path, e.getMessage());
            return Collections.emptyList();
        }
    }

    public static BigDecimal calculateSize(long bytes) {
        return round2(BigDecimal.valueOf(bytes).divide(BigDecimal.valueOf(1024), MATCH_CONTEXT_18));
    }

    public static FileDto buildFileInfo(File file) {
        if (isNull(file)) {
            return null;
        }
        return new FileDto(file);
    }

}
