module com.example.finalproject {
    requires javafx.controls;
    requires javafx.fxml;
    requires atlantafx.base;
    requires javafx.media;
    requires jakarta.persistence;
    requires static lombok;
    requires org.hibernate.orm.core;
    requires spring.boot.autoconfigure;
    requires spring.boot;
    requires spring.data.jpa;
    requires spring.context;
    requires spring.beans;
    requires jaudiotagger;
    requires spring.data.commons;
    requires jdk.httpserver;
    requires uk.co.caprica.vlcj;


    opens com.example.finalproject to javafx.fxml;
    opens com.example.finalproject.controllers to javafx.fxml;
    exports com.example.finalproject;

    opens com.example.finalproject.data.model;
}