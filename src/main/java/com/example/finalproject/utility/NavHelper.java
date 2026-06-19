package com.example.finalproject.utility;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import lombok.Setter;
import org.springframework.context.ApplicationContext;

import java.util.Objects;

public class NavHelper {
    @Setter
    private static ApplicationContext context = null;

    public static void switchScreen(Node clicked) {
        try {
            Button temp = (Button) clicked;
            String target = temp.getText();
            System.out.println(target);

            Stage stage = (Stage) clicked.getScene().getWindow();
            System.out.println("/com/example/finalproject/"+target+".fxml");
            FXMLLoader loader = new FXMLLoader(NavHelper.class.getResource("/com/example/finalproject/"+target+".fxml"));
            loader.setControllerFactory(context::getBean);

            Parent root = loader.load();
            System.out.println("test");
            Scene scene = new Scene(root);
            System.out.println("test2");

            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
