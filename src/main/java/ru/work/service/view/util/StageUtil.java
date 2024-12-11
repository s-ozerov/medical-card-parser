package ru.work.service.view.util;

import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class StageUtil {

    public static Stage setWidthAndHeight(Stage window, int width, int height) {
        window.setMaxWidth(width);
        window.setMinWidth(width);
        window.setMaxHeight(height);
        window.setMinHeight(height);
        return window;
    }

    public static TextArea setWidthAndHeight(TextArea area, int width, int height) {
        area.setMaxWidth(width);
        area.setMinWidth(width);
        area.setMaxHeight(height);
        area.setMinHeight(height);
        return area;
    }

}
