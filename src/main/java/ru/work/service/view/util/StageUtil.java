package ru.work.service.view.util;

import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class StageUtil {

    public static Stage setWidthAndHeight(Stage window, int width, int height) {
        window.setMaxWidth(width);
        window.setMinWidth(width);
        window.setMaxHeight(height);
        window.setMinHeight(height);
        return window;
    }

    public static Pane setWidthAndHeight(Pane pane, int width, int height) {
        pane.setMaxWidth(width);
        pane.setMinWidth(width);
        pane.setMaxHeight(height);
        pane.setMinHeight(height);
        return pane;
    }

    public static TextArea setWidthAndHeight(TextArea area, int width, int height) {
        area.setMaxWidth(width);
        area.setMinWidth(width);
        area.setMaxHeight(height);
        area.setMinHeight(height);
        return area;
    }

    public static Button setWidthAndHeight(Button button, int width, int height) {
        button.setMaxWidth(width);
        button.setMinWidth(width);
        button.setMaxHeight(height);
        button.setMinHeight(height);
        return button;
    }

    public static <T> ListView<T> setWidthAndHeight(ListView<T> listView, int width, int height) {
        listView.setMaxWidth(width);
        listView.setMinWidth(width);
        listView.setMaxHeight(height);
        listView.setMinHeight(height);
        return listView;
    }

}
