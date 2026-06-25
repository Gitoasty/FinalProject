package com.example.finalproject;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import com.example.finalproject.utility.NavHelper;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class HelloApplication extends Application {
    private ConfigurableApplicationContext springContext;

    @Override
    public void init() {
        springContext = SpringApplication.run(Launcher.class);

        NavHelper.setContext(springContext);
    }

    @Override
    public void start(Stage stage) throws IOException {
        Path dbDir = Path.of("data");
        if (Files.notExists(dbDir)) Files.createDirectories(dbDir);

        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        System.out.println(Application.getUserAgentStylesheet());
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("Player.fxml"));
        fxmlLoader.setControllerFactory(springContext::getBean);

        stage.setOnCloseRequest(_ -> System.exit(0));

        Scene scene = new Scene(fxmlLoader.load(), 1280, 720);
        stage.setTitle("FinalProject");
        stage.setMinWidth(750);
        stage.setMinHeight(400);
        stage.setScene(scene);
        stage.show();
    }

    public static void toggleMode() {
        if (Application.getUserAgentStylesheet().equals("/atlantafx/base/theme/primer-dark.css")) {
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        } else if (Application.getUserAgentStylesheet().equals("/atlantafx/base/theme/primer-light.css")) {
            Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        }
    }
}
