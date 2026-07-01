package com.example.finalproject.controllers;

import com.example.finalproject.data.model.Album;
import com.example.finalproject.data.model.Artist;
import com.example.finalproject.data.repository.AlbumRepo;
import com.example.finalproject.data.repository.ArtistRepo;
import com.example.finalproject.utility.SetupHelper;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

@Component
@RequiredArgsConstructor
public class AdditionController implements Initializable {
    @FXML
    private FlowPane navPane;
    @FXML
    private TextField artistField, albumField;
    @FXML
    private ChoiceBox<Artist> artistBox;
    @FXML
    private Rectangle artistIndicator, albumIndicator;

    private final ArtistRepo artistRepo;
    private final AlbumRepo albumRepo;

    public void addArtist() {
        List<Artist> existingArtists = artistRepo.findAll();

        Artist artistToSave = new Artist();
        artistToSave.setName(artistField.getText());

        if (!existingArtists.stream()
                .map(Artist::getName)
                .toList()
                .contains(artistField.getText())) {
            artistRepo.save(artistToSave);

            Platform.runLater(() -> {
                artistIndicator.setVisible(true);

                FadeTransition fade = new FadeTransition(Duration.millis(450), artistIndicator);
                fade.setFromValue(0.0);
                fade.setToValue(1.0);
                fade.setAutoReverse(true);
                fade.setOnFinished(_ -> artistIndicator.setVisible(false));
                fade.play();
            });
        }
    }

    public void addAlbum() {
        List<Album> existingAlbums = albumRepo.findAll();

        Album albumToSave = new Album();
        albumToSave.setName(albumField.getText());
        albumToSave.setArtistId(artistBox.getValue().getId());

        if (!existingAlbums.stream()
                .map(album -> new Album(null, album.getName(), album.getArtistId()))
                .toList()
                .contains(albumToSave)) {
            albumRepo.save(albumToSave);

            Platform.runLater(() -> {
                albumIndicator.setVisible(true);

                FadeTransition fade = new FadeTransition(Duration.millis(450), albumIndicator);
                fade.setFromValue(0.0);
                fade.setToValue(1.0);
                fade.setAutoReverse(true);
                fade.setOnFinished(_ -> albumIndicator.setVisible(false));
                fade.play();
            });
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        navPane.getChildren().add(SetupHelper.prepareNavButton(this, "Import"));
        navPane.getChildren().add(SetupHelper.prepareNavButton(this, "Player"));
        navPane.getChildren().add(SetupHelper.prepareNavButton(this, "EditSongs"));
        navPane.getChildren().add(SetupHelper.prepareToggleButton());

        artistBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Artist a) {
                return a == null ? "" : a.getName();
            }

            @Override
            public Artist fromString(String string) {
                return null;
            }
        });

        List<Artist> tempList = new ArrayList<>();
        tempList.add(new Artist(null, "Choose artist"));
        tempList.addAll(artistRepo.findAll());

        ObservableList<Artist> list = FXCollections.observableArrayList(tempList);

        artistBox.setItems(list);
        artistBox.getSelectionModel().select(0);
    }
}
