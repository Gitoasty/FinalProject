package com.example.finalproject;

import atlantafx.base.theme.PrimerDark;
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
    }

    @Override
    public void start(Stage stage) throws IOException {
        Path dbDir = Path.of("data");
        if (Files.notExists(dbDir)) Files.createDirectories(dbDir);

        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("Player.fxml"));
        fxmlLoader.setControllerFactory(springContext::getBean);

        stage.setOnCloseRequest(e -> System.exit(0));

        Scene scene = new Scene(fxmlLoader.load(), 1280, 720);
        stage.setTitle("FinalProject");
        stage.setScene(scene);
        stage.show();
    }
}
