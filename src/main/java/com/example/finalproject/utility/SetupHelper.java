package com.example.finalproject.utility;

import com.example.finalproject.HelloApplication;
import com.example.finalproject.data.model.Playlist;
import com.example.finalproject.data.model.SongEntry;
import com.example.finalproject.data.repository.PlaylistRepo;
import com.example.finalproject.enums.PlayerMode;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.media.MediaPlayer;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;

import java.util.List;

public interface SetupHelper {
    static Button setupFile(ChoiceBox<Playlist> playlistBox, GridPane parentPane) {
        playlistBox.maxWidthProperty().bind(
                parentPane.widthProperty()
                        .multiply(parentPane.getColumnConstraints()
                                .getFirst()
                                .getPercentWidth() / 100.0)
                        .multiply(0.45)
        );
        GridPane.setHalignment(playlistBox, HPos.RIGHT);

        Button pickerButton = new Button();
        pickerButton.setText("Choose folder");
        pickerButton.maxWidthProperty().bind(
                parentPane.widthProperty()
                        .multiply(parentPane.getColumnConstraints()
                                .getFirst()
                                .getPercentWidth() / 100.0)
                        .multiply(0.45)
        );
        pickerButton.setId("picker");

        parentPane.add(pickerButton, 0, 0);
        GridPane.setHalignment(pickerButton, HPos.LEFT);
        GridPane.setMargin(pickerButton, new Insets(5, 5, 5, 5));

        return pickerButton;
    }

    static void setupDb(ChoiceBox<Playlist> playlistBox, GridPane parentPane) {
        playlistBox.maxWidthProperty().bind(
                parentPane.widthProperty()
                        .multiply(parentPane.getColumnConstraints()
                                .getFirst()
                                .getPercentWidth() / 100.0)
                        .multiply(1)
        );
        GridPane.setHalignment(playlistBox, HPos.CENTER);

        parentPane.getChildren().removeIf(node -> "picker".equals(node.getId()));
    }

    static void generalSetup(Slider progressBar, Slider volumeBar, PlayerMode playerMode,
                             MediaPlayer filePlayer, uk.co.caprica.vlcj.player.base.MediaPlayer dbPlayer,
                             MediaPlayerFactory factory, ListView<SongEntry> songList, FlowPane navPane) {
        progressBar.setMin(0);

        volumeBar.setMin(0);
        volumeBar.setMax(100);

        volumeBar.setValue(100);

        volumeBar.setMajorTickUnit(50);
        volumeBar.setMinorTickCount(10);
        volumeBar.setShowTickMarks(true);
        volumeBar.setShowTickLabels(true);

        volumeBar.valueProperty().addListener((_, _, _) -> {
            if (playerMode == PlayerMode.FILE) {
                filePlayer.setVolume(volumeBar.getValue() * 0.01);
            } else if (playerMode == PlayerMode.DB) {
                if (dbPlayer != null && factory != null) {
                    dbPlayer.audio().setVolume((int) volumeBar.getValue());
                }
            }
        });

        songList.setCellFactory(_ -> new ListCell<>() {
            @Override
            protected void updateItem(SongEntry s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(s.getName());
                }
            }
        });

        navPane.getChildren().add(prepareNavButton("Import"));
        navPane.getChildren().add(prepareToggleButton());
    }

    static Button prepareNavButton(String destination) {
        Button button = new Button();

        button.setText(destination);
        button.setOnAction(event -> NavHelper.switchScreen((Node) event.getSource()));

        return button;
    }

    static Button prepareToggleButton() {
        Button button = new Button();

        button.setText("Light/Dark");
        button.setOnAction(e -> HelloApplication.toggleMode());

        return button;
    }
}
