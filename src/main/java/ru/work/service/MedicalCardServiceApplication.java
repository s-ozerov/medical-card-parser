package ru.work.service;

import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.work.service.view.JavaFxApplication;

@SpringBootApplication
public class MedicalCardServiceApplication {

    public static void main(String[] args) {
        Application.launch(JavaFxApplication.class, args);
    }

}
