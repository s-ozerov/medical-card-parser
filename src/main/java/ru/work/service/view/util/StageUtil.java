package ru.work.service.view.util;

import javafx.stage.Stage;

public class StageUtil {

    public static Stage setWidthAndHeight(Stage window, int width, int height) {
        window.setMaxWidth(width);
        window.setMinWidth(width);
        window.setMaxHeight(height);
        window.setMinHeight(height);
        return window;
    }

}
