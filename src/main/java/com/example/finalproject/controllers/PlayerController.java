package com.example.finalproject.controllers;

import com.example.finalproject.data.model.Playlist;
import com.example.finalproject.data.model.SongEntry;
import com.example.finalproject.data.repository.PlaylistRepo;
import com.example.finalproject.data.repository.PlaylistSongRepo;
import com.example.finalproject.data.repository.SongRepo;
import com.example.finalproject.enums.PlayerMode;
import com.example.finalproject.utility.SetupHelper;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Component
public class PlayerController implements Initializable {
    @FXML
    private Slider volumeBar, progressBar;
    @FXML
    private Label songTitle;
    @FXML
    private ListView<SongEntry> songList;
    @FXML
    private ChoiceBox<Playlist> playlistBox;
    @FXML
    private GridPane parentPane;
    @FXML
    private FlowPane navPane;

    private PlayerMode playerMode;

    private final HashMap<String, String> songs = new HashMap<>();
    private SongEntry selected;
    private MediaPlayer filePlayer;
    private String parentPlaylistPath;

    //For DB playback
    private List<SongRepo.SongSummary> songListDb;
    private uk.co.caprica.vlcj.player.base.MediaPlayer dbPlayer;

    @Autowired
    private SongRepo songRepo;
    @Autowired
    private PlaylistSongRepo playlistSongRepo;
    @Autowired
    private PlaylistRepo playlistRepo;
    private MediaPlayerFactory factory;
    private HttpServer server;

    //DB seeking
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> progressUpdater;
    private volatile boolean seekingByUser = false;

    public void updateListFile(String songFolderPathString) {
        songList.getItems().clear();

        File songFolder = new File(songFolderPathString);

        File[] songFiles = songFolder.listFiles();
        int index = 0;

        if (songFiles != null) {
            for (File song : songFiles) {
                String[] tempArray = song.getName().split("\\.");
                String name = Arrays.toString(Arrays.copyOf(tempArray, tempArray.length - 1)).replace("[", "").replace("]", "");
                songs.put(name, song.getAbsolutePath());
                songList.getItems().add(new SongEntry((long) index, name));

                index++;
            }
        }
    }

    public void updateListDb(Long id) {
        System.out.println("Received id: " + id);
        songListDb = songRepo.findByIdIn(playlistSongRepo.findSongIdsByPlaylistId(id));

        songs.clear();
        songList.getItems().clear();

        for (SongRepo.SongSummary s : songListDb) {
            songs.put(s.getName(), s.getId().toString());
            songList.getItems().add(new SongEntry(s));
        }
    }

    private void setSongFile() {
        Media media = new Media(new File(songs.get(selected.getName())).toURI().toString());
        filePlayer = new MediaPlayer(media);

        filePlayer.currentTimeProperty().addListener((_, _, newTime) -> {
            if (!progressBar.isValueChanging()) {
                progressBar.setValue(newTime.toSeconds());
            }
        });
        filePlayer.setVolume(volumeBar.getValue() * 0.01);

        filePlayer.setOnEndOfMedia(this::nextSong);

        progressBar.setOnMousePressed(_ -> filePlayer.seek(Duration.seconds(progressBar.getValue())));
        progressBar.setOnMouseDragged(_ -> filePlayer.seek(Duration.seconds(progressBar.getValue())));
    }

    private String setSongDb(Long songId) {
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

    private void playPauseDb() {
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

    public void nextSong() {
        if (playerMode == PlayerMode.DB) {
            Optional<SongRepo.SongSummary> current = songListDb.stream().filter(
                    s -> Objects.equals(s.getId(), selected.getId()))
                    .findFirst();

            if (!playlistSongRepo.findSongIdsByPlaylistId(
                            playlistBox.getSelectionModel().getSelectedItem().getId())
                    .contains(selected.getId())) {
                return;
            }

            int index = songListDb.indexOf(current.get());

            if (index==songListDb.size()-1) {
                index = 0;
            } else {
                index++;
            }

            SongEntry next = new SongEntry(
                    songListDb.get(index)
            );

            int indexSong = IntStream.range(0, songList.getItems().size())
                    .filter(i -> songList.getItems().get(i).getId().equals(next.getId())).findFirst()
                    .orElse(-1);

            songList.getSelectionModel().select(indexSong);
            selectSong(next);
        } else if (playerMode == PlayerMode.FILE) {
            int index = songList.getSelectionModel().getSelectedIndex();
            if (index == -1) {
                return;
            }

            Path folder = Paths.get(playlistBox.getSelectionModel().getSelectedItem().getName());
            Path file = folder.resolve(songList.getSelectionModel().getSelectedItem().getName() + ".mp3");

            System.out.println("1");
            System.out.println(new File(file.toUri()).getAbsolutePath());
            if (Files.exists(file) && Files.isRegularFile(file)) {
                System.out.println("2");
                if (index == songList.getItems().size()-1) {
                    index = 0;
                } else {
                    index++;
                }

                songList.getSelectionModel().select(index);
                selectSong(songList.getSelectionModel().getSelectedItem());
            }
        }
    }

    public void prevSong() {
        if (playerMode == PlayerMode.DB) {
            Optional<SongRepo.SongSummary> current = songListDb.stream().filter(
                            s -> Objects.equals(s.getId(), selected.getId()))
                    .findFirst();
            int index = songListDb.indexOf(current.get());

            if (index==0) {
                index = songListDb.size()-1;
            } else {
                index--;
            }

            SongEntry prev = new SongEntry(
                    songListDb.get(index)
            );

            int indexSong = IntStream.range(0, songList.getItems().size())
                    .filter(i -> songList.getItems().get(i).getId().equals(prev.getId())).findFirst()
                    .orElse(-1);

            songList.getSelectionModel().select(indexSong);

            selectSong(prev);
        } else if (playerMode == PlayerMode.FILE) {
            int index = songList.getSelectionModel().getSelectedIndex();

            if (index == 0) {
                index = songList.getItems().size()-1;
            } else {
                index--;
            }

            songList.getSelectionModel().select(index);
            selectSong(songList.getSelectionModel().getSelectedItem());
        }
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

    private void selectSong(SongEntry songEntry) {
        songTitle.setText(songEntry.getName());
        System.out.println("Selected: " + songEntry);

        if (playerMode == PlayerMode.FILE) {
            if (selected != null) {
                stopFile();
            }

            selected = songEntry;
            setSongFile();
            playPauseFile();
        } else if (playerMode == PlayerMode.DB) {
            if (!songListDb.stream().map(SongRepo.SongSummary::getId).toList().contains(songEntry.getId())) return;

            selected = songEntry;
            String songUrl = setSongDb(songEntry.getId());
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
                if (length > 2 && (length - posSeconds < 1)) {
                    nextSong();
                }

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
        progressBar.setOnMousePressed(_ -> {
            seekingByUser = true;
            double targetSec = progressBar.getValue();
            dbPlayer.controls().setTime((long)(targetSec * 1000));
        });

        progressBar.setOnMouseReleased(_ -> {
            double targetSec = progressBar.getValue();
            dbPlayer.controls().setTime((long)(targetSec * 1000));
            seekingByUser = false;
        });

        progressBar.setOnMouseDragged(_ -> {
            double targetSec = progressBar.getValue();
            if (seekingByUser) dbPlayer.controls().setTime((long)(targetSec * 1000));
        });
    }

    private void choose(ActionEvent e) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select playlist folder");

        Button source = (Button) e.getSource();

        String selectedDirectory = String.valueOf(directoryChooser.showDialog(source.getScene().getWindow()));
        parentPlaylistPath = selectedDirectory;

        if (selectedDirectory != null) {
            setPlaylistsFile(selectedDirectory);
        }
    }

    private void setPlaylistsFile(String selectedDirectory) {
        File rootFolder = new File(selectedDirectory);
        File[] playlistFolders = rootFolder.listFiles(File::isDirectory);

        if (playlistFolders != null) {
            List<String> tempList = Arrays.stream(playlistFolders)
                    .map(File::getAbsolutePath)
                    .toList();

            List<Playlist> insertList = new ArrayList<>();

            for (int i = 0; i< tempList.size(); i++) {
                insertList.add(new Playlist((long) i, tempList.get(i)));
            }

            ObservableList<Playlist> list = FXCollections.observableArrayList(insertList);

            playlistBox.getItems().addAll(list);
        }
    }

    private void setPlaylistsDb() {
        List<Playlist> tempList = new ArrayList<>();
        tempList.add(new Playlist(null, "Choose playlist"));
        tempList.addAll(playlistRepo.findAll());

        ObservableList<Playlist> list = FXCollections.observableArrayList(tempList);

        playlistBox.setItems(list);
        playlistBox.getSelectionModel().select(0);
    }

    private void choosePlaylist() {
        Playlist selected = playlistBox.getSelectionModel().getSelectedItem();

        if (playerMode == PlayerMode.DB) {
            updateListDb(selected.getId());
        } else if (playerMode == PlayerMode.FILE) {
            if (selected != null) {
                updateListFile(selected.getName());
            }
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        playerMode = PlayerMode.DB;

        factory = new MediaPlayerFactory();
        dbPlayer = factory.mediaPlayers().newMediaPlayer();

        if (playerMode == PlayerMode.FILE) {
            playlistBox.getItems().add(new Playlist(null, "Choose playlist"));
            playlistBox.getSelectionModel().select(0);

            SetupHelper.setupFile(playlistBox, parentPane).setOnAction(this::choose);
        } else if (playerMode == PlayerMode.DB) {
            setPlaylistsDb();
        }

        SetupHelper.generalSetup(progressBar, volumeBar, playerMode, filePlayer, dbPlayer, factory, songList);

        songList.getSelectionModel().selectedItemProperty().addListener((_, _, newVal) -> {
            if (newVal != null) {
                selectSong(newVal);
            }
        });

        playlistBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Playlist p) {
                return p == null ? "" : p.getName();
            }

            @Override
            public Playlist fromString(String string) {
                return null;
            }
        });
        playlistBox.setOnAction(_ -> choosePlaylist());
    }
}
