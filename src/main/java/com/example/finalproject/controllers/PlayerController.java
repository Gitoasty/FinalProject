package com.example.finalproject.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ResourceBundle;

public class PlayerController implements Initializable {
    @FXML
    private Slider volumeBar, progressBar;
    @FXML
    private Label songTitle;
    @FXML
    private Button prevButton, nextButton;
    @FXML
    private ListView<String> songList;

    private HashMap<String, String> songs = new HashMap<>();
    private String selected;
    private Media media;
    private MediaPlayer mediaPlayer;

    public void updateList() {
        songList.getItems().clear();
        String songFolderPathString = "G:\\downloaded_music\\Anymez";
        Path songFolderPath = Path.of(songFolderPathString);

        File songFolder = new File(songFolderPathString);

        File[] songFiles = songFolder.listFiles();

        for (File song : songFiles) {
            String[] tempArray = song.getName().split("\\.");
            String name = Arrays.toString(Arrays.copyOf(tempArray, tempArray.length - 1)).replace("[", "").replace("]", "");
            System.out.println(name);
            songs.put(name, song.getAbsolutePath());
            songList.getItems().add(name);
        }
    }

    private void setSong() {
        media = new Media(new File(songs.get(selected)).toURI().toString());
        mediaPlayer = new MediaPlayer(media);

        mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (!progressBar.isValueChanging()) {
                progressBar.setValue(newTime.toSeconds());
            }
        });
        mediaPlayer.setVolume(volumeBar.getValue() * 0.01);

        progressBar.setOnMousePressed(e -> mediaPlayer.seek(Duration.seconds(progressBar.getValue())));
        progressBar.setOnMouseDragged(e -> mediaPlayer.seek(Duration.seconds(progressBar.getValue())));
    }

    public void playPause() {
        if (mediaPlayer.getStatus().equals(MediaPlayer.Status.PLAYING)) {
            mediaPlayer.pause();
        } else if (mediaPlayer.getStatus().equals(MediaPlayer.Status.PAUSED)) {
            mediaPlayer.play();
        } else {
            mediaPlayer.setOnReady(() -> {
                Duration total = mediaPlayer.getTotalDuration();
                progressBar.setMax(total.toSeconds());
                progressBar.setValue(0);
            });

            mediaPlayer.play();
        }
    }

    private void stop() {
        mediaPlayer.stop();
        mediaPlayer.dispose();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        updateList();

        volumeBar.setMin(0);
        volumeBar.setMax(100);
        volumeBar.setValue(100);
        volumeBar.setMajorTickUnit(50);
        volumeBar.setMinorTickCount(10);
        volumeBar.setShowTickMarks(true);
        volumeBar.setShowTickLabels(true);

        volumeBar.valueProperty().addListener((observableValue, number, t1) -> mediaPlayer.setVolume(volumeBar.getValue() * 0.01));

        progressBar.setMin(0);

        songList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                songTitle.setText(newVal);
                System.out.println("Selected: " + newVal);

                if (selected != null) {
                    stop();
                }

                selected = newVal;
                setSong();
                playPause();
            }
        });
    }
}
