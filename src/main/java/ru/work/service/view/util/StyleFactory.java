package ru.work.service.view.util;

import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.layout.Pane;
import lombok.extern.slf4j.Slf4j;
import ru.work.service.anotations.Css;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import static ru.work.service.view.util.Constants.CURRENT_THEME;
import static ru.work.service.view.util.Constants.PATCH_CSS;
import static ru.work.service.view.util.Theme.LIGHT;

@Slf4j
public class StyleFactory {

    public static void setTheme(Object controller) {
        for (Field field : controller.getClass().getDeclaredFields()) {
            Annotation[] annotations = field.getDeclaredAnnotations();
            for (Annotation annotation : annotations) {
                if (annotation.annotationType() == Css.class) {
                    try {
                        Css css = field.getAnnotation(Css.class);

                        field.setAccessible(true);
                        log.info("component type: {}", field.getType());
                        Control component = (Control) field.get(controller);

                        component.getStylesheets().clear();
                        if (CURRENT_THEME == LIGHT) {
                            component.getStylesheets().add(PATCH_CSS + css.light());
                        } else {
                            component.getStylesheets().add(PATCH_CSS + css.dark());
                        }
                    } catch (Exception e) {
                        log.error("failed set theme: {}", e.getMessage());
                    }
                }
            }
        }
    }

    public static void removeBackground(Button button) {
        button.setStyle("""
                -fx-background-color: transparent;
                -fx-cursor: hand;
                """);
    }

    public static void setSize(Control component, double width, double height) {
        component.setMaxWidth(width);
        component.setMinWidth(width);
        component.setMaxHeight(height);
        component.setMinHeight(height);
    }

    public static void setSize(Pane pane, double width, double height) {
        pane.setMaxWidth(width);
        pane.setMinWidth(width);
        pane.setMaxHeight(height);
        pane.setMinHeight(height);
    }

    public static void setSize(Control component, double minWidth, double maxWidth, double minHeight, double maxHeight) {
        component.setMaxWidth(maxWidth);
        component.setMinWidth(minWidth);
        component.setMaxHeight(maxHeight);
        component.setMinHeight(minHeight);
    }

    public static void setSize(Pane pane, double minWidth, double maxWidth, double minHeight, double maxHeight) {
        pane.setMaxWidth(maxWidth);
        pane.setMinWidth(minWidth);
        pane.setMaxHeight(maxHeight);
        pane.setMinHeight(minHeight);
    }

}
