package com.example.finalproject.utility;

import com.example.finalproject.HelloApplication;
import com.example.finalproject.data.model.Playlist;
import com.example.finalproject.data.model.SongEntry;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;

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

    static void generalSetup(Slider progressBar, Slider volumeBar,
                             ListView<SongEntry> songList, FlowPane navPane) {
        progressBar.setMin(0);

        volumeBar.setMin(0);
        volumeBar.setMax(100);

        volumeBar.setValue(100);

        volumeBar.setMajorTickUnit(50);
        volumeBar.setMinorTickCount(10);
        volumeBar.setShowTickMarks(true);
        volumeBar.setShowTickLabels(true);

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

    static boolean isLinux() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("linux");
    }
}
