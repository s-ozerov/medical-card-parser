package ru.work.service.view.util;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static ru.work.service.view.util.Constants.PATCH_IMAGE;

@Slf4j
public class ImageFactory {

    @RequiredArgsConstructor
    public enum ImageName {
        ADD("add-48.png"),
        EDIT("edit-48.png"),
        REMOVE("remove-48.png"),
        DOWNLOAD("download-32.png"),
        WORD("word.png"),
        FOLDER("folder-48.png"),
        OPEN("open-48.png"),
        DELETE("delete-icon.png");

        private final String name;

        public String getName() {
            return PATCH_IMAGE + name;
        }
    }

    public static ImageView buildSize20(ImageName imageName) {
        return build(imageName, 20, 20);
    }

    public static ImageView buildSize24(ImageName imageName) {
        return build(imageName, 24, 24);
    }

    public static ImageView buildSize36(ImageName imageName) {
        return build(imageName, 36, 36);
    }

    public static ImageView buildSize48(ImageName imageName) {
        return build(imageName, 48, 48);
    }

    public static ImageView build(ImageName imageName, double width, double height) {
        ClassLoader classLoader = ResourceLoader.class.getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(imageName.getName())) {
            if (inputStream == null) {
                throw new FileNotFoundException(imageName.getName());
            }
            Image icon = new Image(inputStream);
            ImageView iconView = new ImageView(icon);
            iconView.setFitWidth(width);
            iconView.setFitHeight(height);
            return iconView;
        } catch (IOException e) {
            log.error("error: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }


}
