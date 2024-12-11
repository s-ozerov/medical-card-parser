package ru.work.service.service.sheet;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.work.service.dto.DownloadDto;
import ru.work.service.dto.ProcessResponse;
import ru.work.service.dto.medical.MedicalDocFile;
import ru.work.service.rest.CurrentTimeRestAdapter;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Disabled
@SpringBootTest
class MedicalSheetFileHandlerTest {

    @Autowired
    private MedicalSheetFileHandler medicalSheetFileHandler;

    @Autowired
    private CurrentTimeRestAdapter currentTimeRestAdapter;

    @Test
    @SneakyThrows
    public void test() {
        String path = "C:\\Users\\Albion\\Desktop\\тест";
//        String path = "C:\\Users\\Albion\\Desktop\\Июль";
        ProcessResponse<MedicalDocFile> content = medicalSheetFileHandler.readFile(path);
        Assertions.assertNotNull(content);

        String filename = DateTimeFormatter.ofPattern("dd MMMM HH-mm-ss").format(LocalDateTime.now()) + ".xlsx";
        DownloadDto download = medicalSheetFileHandler.convertDOCToXLSX(filename, content);

        File file = new File(path + "\\" + filename);
        FileUtils.copyInputStreamToFile(download.getContent(), file);
    }

    @Test
    @SneakyThrows
    public void timeTest() {
        LocalDateTime time = currentTimeRestAdapter.getCurrentTime();
        Assertions.assertNotNull(time);
        log.info("Current time: {}", time);
    }

}