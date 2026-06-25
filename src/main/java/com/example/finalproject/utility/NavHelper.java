package com.example.finalproject.utility;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import lombok.Setter;
import org.springframework.context.ApplicationContext;

public class NavHelper {
    @Setter
    private static ApplicationContext context = null;

    public static void switchScreen(Node clicked) {
        try {
            Button temp = (Button) clicked;
            String target = temp.getText();

            Stage stage = (Stage) clicked.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(NavHelper.class.getResource("/com/example/finalproject/"+target+".fxml"));
            loader.setControllerFactory(context::getBean);

            Parent root = loader.load();
            Scene scene = new Scene(root);

            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {e.printStackTrace();}
    }
}
