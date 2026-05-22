package com.example.finalproject.controllers;

import com.example.finalproject.data.model.Playlist;
import com.example.finalproject.data.repository.PlaylistSongRepo;
import com.example.finalproject.data.repository.SongRepo;
import com.example.finalproject.enums.PlayerMode;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
public class PlayerController implements Initializable {
    @FXML
    private Slider volumeBar, progressBar;
    @FXML
    private Label songTitle;
    @FXML
    private Button prevButton, nextButton;
    @FXML
    private ListView<String> songList;
    private ScheduledExecutorService uiUpdater;

    private PlayerMode playerMode;

    private HashMap<String, String> songs = new HashMap<>();
    private String selected;
    private Media media;
    private MediaPlayer filePlayer;
    private List<Playlist> playlists;

    //For DB playback
    private List<SongRepo.SongSummary> songSummaries;
    private uk.co.caprica.vlcj.player.base.MediaPlayer dbPlayer;

    @Autowired
    private SongRepo songRepo;
    @Autowired
    private PlaylistSongRepo playlistSongRepo;
    private MediaPlayerFactory factory;
    private HttpServer server;

    //DB seeking
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> progressUpdater;
    private volatile boolean seekingByUser = false;

    public void updateListFile() {
        songList.getItems().clear();
        String songFolderPathString = "G:\\downloaded_music\\Anymez";

        File songFolder = new File(songFolderPathString);

        File[] songFiles = songFolder.listFiles();

        for (File song : songFiles) {
            String[] tempArray = song.getName().split("\\.");
            String name = Arrays.toString(Arrays.copyOf(tempArray, tempArray.length - 1)).replace("[", "").replace("]", "");
            songs.put(name, song.getAbsolutePath());
            songList.getItems().add(name);
        }
    }

    public void updateListDb() {
        songSummaries = songRepo.findByIdIn(playlistSongRepo.findSongIdsByPlaylistId(1L));

        for (SongRepo.SongSummary s : songSummaries) {
            songs.put(s.getName(), s.getId().toString());
            songList.getItems().add(s.getName());
        }
    }

    private void setSongFile() {
        media = new Media(new File(songs.get(selected)).toURI().toString());
        filePlayer = new MediaPlayer(media);

        filePlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (!progressBar.isValueChanging()) {
                progressBar.setValue(newTime.toSeconds());
            }
        });
        filePlayer.setVolume(volumeBar.getValue() * 0.01);

        progressBar.setOnMousePressed(e -> filePlayer.seek(Duration.seconds(progressBar.getValue())));
        progressBar.setOnMouseDragged(e -> filePlayer.seek(Duration.seconds(progressBar.getValue())));
    }

    private String setSongDb(String songName) {
        Long songId = Long.valueOf(songs.get(songName));
        byte[] songData = songRepo.findDataById(songId);

        return serveSong(songData);
    }

    public void playPause() {
        if (playerMode == PlayerMode.FILE) {
            playPauseFile();
        } else if (playerMode == PlayerMode.DB) {
            playPauseDb();
        }
    }

    private void playPauseFile() {
        if (filePlayer.getStatus().equals(MediaPlayer.Status.PLAYING)) {
            filePlayer.pause();
        } else if (filePlayer.getStatus().equals(MediaPlayer.Status.PAUSED)) {
            filePlayer.play();
        } else {
            filePlayer.setOnReady(() -> {
                Duration total = filePlayer.getTotalDuration();
                progressBar.setMax(total.toSeconds());
                progressBar.setValue(0);
            });

            filePlayer.play();
        }
    }

    private void playPauseDb() { //TODO test this
        if (dbPlayer.status().isPlaying()) {
            dbPlayer.controls().pause();
            return;
        }

        if (dbPlayer.status().isPlayable()) {
            dbPlayer.controls().play();
        }
    }

    private void stopFile() {
        filePlayer.stop();
        filePlayer.dispose();
    }

    private void playDb(String songUrl) {
        factory = new MediaPlayerFactory();
        dbPlayer = factory.mediaPlayers().newMediaPlayer();
        dbPlayer.media().play(songUrl);

        setDbProgress();
        startDbProgressUpdater();
    }

    private void stopDb() {
        dbPlayer.controls().stop();
        server.stop(0);
        stopDbProgressUpdater();
    }

    private String serveSong(byte[] songData) {
        try {
            if (server != null) {
                stopDb();

                server = null;
                System.out.println("Stopped serving");
            }

            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            String path = "/song.mp3";
            server.createContext(path, new RangeHandler(songData, "audio/mpeg"));
            server.setExecutor(Executors.newSingleThreadExecutor());
            server.start();

            int port = server.getAddress().getPort();
            String url = "http://127.0.0.1:" + port + path;
            System.out.println("Serving blob at: " + url);

            System.setProperty(
                    "jna.library.path",
                    "C:\\Program Files\\VideoLAN\\VLC"
            );

            return url;
        } catch (Exception _) {
            return "";
        }
    }

    static class RangeHandler implements HttpHandler {
        private final byte[] data;
        private final String contentType;

        RangeHandler(byte[] data, String contentType) {
            this.data = data;
            this.contentType = contentType;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Headers resp = exchange.getResponseHeaders();
            resp.set("Content-Type", contentType);
            long fileLen = data.length;

            String range = exchange.getRequestHeaders().getFirst("Range");
            long start = 0, end = fileLen - 1;
            if (range != null && range.startsWith("bytes=")) {
                String[] parts = range.substring(6).split("-", 2);
                try {
                    if (!parts[0].isEmpty()) start = Long.parseLong(parts[0]);
                    if (parts.length > 1 && !parts[1].isEmpty()) end = Long.parseLong(parts[1]);
                } catch (NumberFormatException ignored) {}
                if (start < 0) start = 0;
                if (end >= fileLen) end = fileLen - 1;
                if (start > end) {
                    exchange.sendResponseHeaders(416, -1);
                    exchange.close();
                    return;
                }
                resp.set("Accept-Ranges", "bytes");
                resp.set("Content-Range", "bytes " + start + "-" + end + "/" + fileLen);
                long contentLen = end - start + 1;
                exchange.sendResponseHeaders(206, contentLen);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(data, (int) start, (int) contentLen);
                }
            } else {
                exchange.sendResponseHeaders(200, fileLen);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(data);
                }
            }
            exchange.close();
        }
    }

    private void selectSong(String songName) {
        songTitle.setText(songName);
        System.out.println("Selected: " + songName);

        if (playerMode == PlayerMode.FILE) {
            if (selected != null) {
                stopFile();
            }

            selected = songName;
            setSongFile();
            playPauseFile();
        } else if (playerMode == PlayerMode.DB) {
            selected = songName;
            String songUrl = setSongDb(songName);
            System.out.println("working up to here");
            if (!songUrl.isEmpty()) {
                System.out.println("song url is: " + songUrl);
                playDb(songUrl);
            }
        }
    }

    private void startDbProgressUpdater() {
        if (progressUpdater != null && !progressUpdater.isDone()) {
            progressUpdater.cancel(true);
        }

        long lengthMs = dbPlayer.media().info().duration();
        if (lengthMs > 0) {
            progressBar.setMax(lengthMs / 1000.0);
        }

        progressUpdater = scheduler.scheduleAtFixedRate(() -> {
            double length = dbPlayer.media().info().duration() / 1000.0;
            double posSeconds = dbPlayer.status().time() / 1000.0;

            javafx.application.Platform.runLater(() -> {
                if (length > 0) {
                    progressBar.setMax(length);
                }
                if (!seekingByUser) {
                    progressBar.setValue(posSeconds);
                }
            });
        }, 0, 50, TimeUnit.MILLISECONDS);
    }

    private void stopDbProgressUpdater() {
        if (progressUpdater != null) {
            progressUpdater.cancel(true);
            progressUpdater = null;
        }
    }

    private void setDbProgress() {
        progressBar.setOnMousePressed(e -> {
            seekingByUser = true;
            double targetSec = progressBar.getValue();
            dbPlayer.controls().setTime((long)(targetSec * 1000));
        });

        progressBar.setOnMouseReleased(e -> {
            double targetSec = progressBar.getValue();
            dbPlayer.controls().setTime((long)(targetSec * 1000));
            seekingByUser = false;
        });

        progressBar.setOnMouseDragged(e -> {
            double targetSec = progressBar.getValue();
            if (seekingByUser) dbPlayer.controls().setTime((long)(targetSec * 1000));
        });
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        playerMode = PlayerMode.DB;

        factory = new MediaPlayerFactory();
        dbPlayer = factory.mediaPlayers().newMediaPlayer();

        if (playerMode == PlayerMode.FILE) {
            updateListFile();
        } else if (playerMode == PlayerMode.DB) {
            updateListDb();
        }

        progressBar.setMin(0);

        volumeBar.setMin(0);
        volumeBar.setMax(100);

        if (playerMode == PlayerMode.FILE) {
            volumeBar.setValue(100);
        } else if (playerMode == PlayerMode.DB) {
            volumeBar.setValue(dbPlayer.audio().volume());
        }

        volumeBar.setMajorTickUnit(50);
        volumeBar.setMinorTickCount(10);
        volumeBar.setShowTickMarks(true);
        volumeBar.setShowTickLabels(true);

        volumeBar.valueProperty().addListener((observableValue, number, t1) -> {
            if (playerMode == PlayerMode.FILE) {
                filePlayer.setVolume(volumeBar.getValue() * 0.01);
            } else if (playerMode == PlayerMode.DB) {
                if (dbPlayer != null && factory != null) {
                    dbPlayer.audio().setVolume((int) volumeBar.getValue());
                }
            }
        });

        songList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectSong(newVal);
            }
        });
    }
}
