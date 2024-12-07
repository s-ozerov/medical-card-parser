package ru.work.service.view;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.rgielen.fxweaver.core.FxWeaver;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import ru.work.service.MedicalCardServiceApplication;
import ru.work.service.view.controller.AuthController;
import ru.work.service.view.util.Constants;
import ru.work.service.view.util.Theme;

import static ru.work.service.view.util.Constants.CURRENT_THEME;
import static ru.work.service.view.util.StageUtil.setWidthAndHeight;

public class JavaFxApplication extends Application {

    public static Stage WINDOW;
    public static ConfigurableApplicationContext APPLICATION_CONTEXT;

    @Override
    public void init() {
        String[] args = getParameters().getRaw().toArray(new String[0]);

        APPLICATION_CONTEXT = new SpringApplicationBuilder()
                .sources(MedicalCardServiceApplication.class)
                .run(args);
    }

    @Override
    public void start(Stage window) {
        FxWeaver fxWeaver = APPLICATION_CONTEXT.getBean(FxWeaver.class);
        Parent root = fxWeaver.loadView(AuthController.class);
        Scene scene = new Scene(root);
        window.initStyle(StageStyle.DECORATED);
        window.setScene(scene);
        setWidthAndHeight(window, 500, 200);
        window.getIcons().add(new Image(Constants.MAIN_ICO));
        WINDOW = window;
        window.show();

        if (CURRENT_THEME == Theme.DARK) {
            Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        } else {
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        }
    }

    @Override
    public void stop() {
        APPLICATION_CONTEXT.close();
        Platform.exit();
        System.exit(0);
    }

}
