package com.example.finalproject;

import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Launcher {
    static void main(String[] args) {
        Application.launch(HelloApplication.class, args);
    }
}
