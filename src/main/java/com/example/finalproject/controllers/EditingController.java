package com.example.finalproject.controllers;

import com.example.finalproject.data.UtilService;
import com.example.finalproject.data.model.Album;
import com.example.finalproject.data.model.Artist;
import com.example.finalproject.data.model.Song;
import com.example.finalproject.data.repository.AlbumRepo;
import com.example.finalproject.data.repository.ArtistRepo;
import com.example.finalproject.data.repository.PlaylistSongRepo;
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
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    @FXML
    private ToggleButton likedToggle;

    private final SongRepo songRepo;
    private final PlaylistSongRepo playlistSongRepo;
    private final ArtistRepo artistRepo;
    private final AlbumRepo albumRepo;
    private final UtilService utilService;

    private void reloadTable() {
        List<SongRepo.SongInfo> songs = songRepo.findAllSongInfoProjectedBy();

        table.getItems().clear();

        id.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getId()));
        name.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getName()));
        artist.setCellValueFactory(c -> new SimpleStringProperty(utilService.getArtist(c.getValue().getArtistId())));
        album.setCellValueFactory(c -> new SimpleStringProperty(utilService.getAlbum(c.getValue().getAlbumId())));
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

    private void populateArtists() {
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

    private void populateAlbums() {
        albumBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Album a) {
                return a == null ? "" : a.getName();
            }

            @Override
            public Album fromString(String string) {
                return null;
            }
        });

        List<Album> tempList = new ArrayList<>();
        tempList.add(new Album(null, "Choose album", null));
        tempList.addAll(albumRepo.findAll());

        ObservableList<Album> list = FXCollections.observableArrayList(tempList);

        albumBox.setItems(list);
        albumBox.getSelectionModel().select(0);
    }

    public void updateSong() {
        if (table.getSelectionModel().getSelectedItem() != null) {
            Optional<Song> song = songRepo.findById(table.getSelectionModel().getSelectedItem().getId());
            song.get().setName(nameField.getText());
            song.get().setArtistId(artistBox.getValue().getId());
            song.get().setAlbumId(albumBox.getValue().getId());
            song.get().setLength(table.getSelectionModel().getSelectedItem().getLength());
            song.get().setReleaseYear(yearField.getText());
            song.get().setNote(noteField.getText());
            song.get().setLiked(likedToggle.isSelected());

            songRepo.saveAndFlush(song.get());

            reloadTable();

            idLabel.setText("Song id: ");
            nameField.setText("");
            artistBox.getSelectionModel().select(0);
            albumBox.getSelectionModel().select(0);
            yearField.setText("");
            noteField.setText("");
            likedToggle.setSelected(false);
        }
    }

    public void deleteSong() {
        playlistSongRepo.deleteBySongId(table.getSelectionModel().getSelectedItem().getId());
        songRepo.deleteById(table.getSelectionModel().getSelectedItem().getId());

        reloadTable();

        idLabel.setText("Song id: ");
        nameField.setText("");
        artistBox.getSelectionModel().select(0);
        albumBox.getSelectionModel().select(0);
        yearField.setText("");
        noteField.setText("");
        likedToggle.setSelected(false);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        navPane.getChildren().add(SetupHelper.prepareNavButton("Import"));
        navPane.getChildren().add(SetupHelper.prepareNavButton("Player"));
        navPane.getChildren().add(SetupHelper.prepareNavButton("Additions"));
        navPane.getChildren().add(SetupHelper.prepareToggleButton());

        populateArtists();
        populateAlbums();
        reloadTable();

        table.getSelectionModel()
                .selectedItemProperty()
                .addListener((_, _, value) -> {
                    if (value != null) {
                        idLabel.setText("Selected id: " + value.getId());
                        nameField.setText(value.getName());
                        artistBox.getSelectionModel().select(
                                artistBox.getItems().stream()
                                        .filter(a -> a.getName().equals(utilService.getArtist(value.getArtistId())))
                                        .findFirst()
                                        .orElse(null)
                        );
                        albumBox.getSelectionModel().select(
                                albumBox.getItems().stream()
                                        .filter(a -> a.getName().equals(utilService.getAlbum(value.getAlbumId())))
                                        .findFirst()
                                        .orElse(null)
                        );
                        yearField.setText(value.getReleaseYear());
                        noteField.setText(value.getNote());
                        likedToggle.setSelected(value.getLiked());
                    }
                });
    }
}
