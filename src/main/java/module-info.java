module com.example.finalproject {
    requires javafx.controls;
    requires javafx.fxml;
    requires atlantafx.base;
    requires javafx.media;


    opens com.example.finalproject to javafx.fxml;
    opens com.example.finalproject.controllers to javafx.fxml;
    exports com.example.finalproject;
}