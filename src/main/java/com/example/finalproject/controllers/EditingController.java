package com.example.finalproject.controllers;

import com.example.finalproject.data.model.Album;
import com.example.finalproject.data.model.Artist;
import com.example.finalproject.data.model.Song;
import com.example.finalproject.utility.SetupHelper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;

@Component
@RequiredArgsConstructor
public class EditingController implements Initializable {
    @FXML
    private FlowPane navPane;
    @FXML
    private TableView<Song> table;
    @FXML
    private TableColumn<Song, Long> id, plays;
    @FXML
    private TableColumn<Song, String> name, artist, album, length, year, note;
    @FXML
    private TableColumn<Song, Boolean> liked;
    @FXML
    private Label idLabel;
    @FXML
    private TextField nameField, yearField, noteField;
    @FXML
    private ChoiceBox<Artist> artistBox;
    @FXML
    private ChoiceBox<Album> albumBox;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        navPane.getChildren().add(SetupHelper.prepareNavButton("Import"));
        navPane.getChildren().add(SetupHelper.prepareNavButton("Player"));
        navPane.getChildren().add(SetupHelper.prepareToggleButton());
    }
}
