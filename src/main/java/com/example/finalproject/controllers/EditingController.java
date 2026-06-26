package com.example.finalproject.controllers;

import com.example.finalproject.data.UtilService;
import com.example.finalproject.data.model.Album;
import com.example.finalproject.data.model.Artist;
import com.example.finalproject.data.model.SongDto;
import com.example.finalproject.data.repository.AlbumRepo;
import com.example.finalproject.data.repository.ArtistRepo;
import com.example.finalproject.data.repository.SongRepo;
import com.example.finalproject.utility.SetupHelper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

@Component
@RequiredArgsConstructor
public class EditingController implements Initializable {
    @FXML
    private FlowPane navPane;
    @FXML
    private TableView<SongRepo.SongInfo> table;
    @FXML
    private TableColumn<SongRepo.SongInfo, Long> id, plays;
    @FXML
    private TableColumn<SongRepo.SongInfo, String> name, artist, album, length, year, note;
    @FXML
    private TableColumn<SongRepo.SongInfo, Boolean> liked;
    @FXML
    private Label idLabel;
    @FXML
    private TextField nameField, yearField, noteField;
    @FXML
    private ChoiceBox<Artist> artistBox;
    @FXML
    private ChoiceBox<Album> albumBox;

    private final SongRepo songRepo;
    private final UtilService utilService;

    private void reloadTable() {
        List<SongRepo.SongInfo> songs = songRepo.findAllSongInfoProjectedBy();

        table.getItems().clear();

        id.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getId()));
        name.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getName()));
        artist.setCellValueFactory(c -> new SimpleStringProperty(utilService.getArtist(c.getValue().getArtistId())));
        album.setCellValueFactory(c -> new SimpleStringProperty(utilService.getArtist(c.getValue().getAlbumId())));
        length.setCellValueFactory(c -> {
            int time = Integer.parseInt(c.getValue().getLength().toString());

            String timeActual = String.format("%02d:%02d", time/60, time%60);
            return new SimpleStringProperty(timeActual);
        });
        plays.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getPlays()));
        year.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getReleaseYear()));
        note.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getNote()));
        liked.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getLiked()));

        ObservableList<SongRepo.SongInfo> infos = FXCollections.observableArrayList(songs);
        table.setItems(infos);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        navPane.getChildren().add(SetupHelper.prepareNavButton("Import"));
        navPane.getChildren().add(SetupHelper.prepareNavButton("Player"));
        navPane.getChildren().add(SetupHelper.prepareNavButton("Additions"));
        navPane.getChildren().add(SetupHelper.prepareToggleButton());

        reloadTable();
    }
}
