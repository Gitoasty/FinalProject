package com.example.finalproject.controllers;

import com.example.finalproject.data.model.*;
import com.example.finalproject.data.repository.*;
import com.example.finalproject.utility.SetupHelper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javafx.scene.layout.FlowPane;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ImportController implements Initializable {
    @FXML
    private Button chooseButton;
    @FXML
    private Label playlistName;
    @FXML
    private Slider importProgress;
    @FXML
    private FlowPane navPane;
    @FXML
    private Label done;

    @Autowired
    private SongRepo songRepo;
    @Autowired
    private PlaylistRepo playlistRepo;
    @Autowired
    private ArtistRepo artistRepo;
    @Autowired
    private AlbumRepo albumRepo;
    @Autowired
    private PlaylistSongRepo playlistSongRepo;

    private File selectedDirectory;

    private final AtomicInteger progress = new AtomicInteger(0);
    private final ExecutorService importExecutor = Executors.newSingleThreadExecutor();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> progressUpdater;

    public void choose() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select playlist folder");

        selectedDirectory = directoryChooser.showDialog(chooseButton.getScene().getWindow());

        if (selectedDirectory != null) {
            playlistName.setText(selectedDirectory.getName());
        }
    }

    public void importPlaylist() {
        if (selectedDirectory != null) {
            File[] songs = selectedDirectory.listFiles();

            if (songs != null) {
                importExecutor.submit(() -> performImport(songs));
            }
        }
    }

    private void performImport(File[] songs) {
        progress.set(0);
        Platform.runLater(() -> {
            importProgress.setMax(songs.length);
            importProgress.setValue(0);

            done.setText("Processing...");
        });
        startProgressUpdater();

        Playlist playlist = new Playlist();
        playlist.setName(selectedDirectory.getName());

        Optional<Playlist> existingPlaylist = playlistRepo.findByName(selectedDirectory.getName());
        if (existingPlaylist.isEmpty()) {
            playlist = playlistRepo.save(playlist);
        } else {
            playlist = existingPlaylist.get();
        }

        List<SongRepo.SongSummary> existingSongs = songRepo.findAllProjectedBy();
        existingSongs.forEach(System.out::println);
        List<Artist> existingArtists = artistRepo.findAll();
        List<Album> existingAlbums = albumRepo.findAll();

        int batchSize = 50;
        List<PlaylistSong> linkBatch = new ArrayList<>(batchSize);

        for (File song : songs) {
            if (song.length() < 1000L) {
                System.out.println("File is corrupted or empty - " + song.getName());
                progress.incrementAndGet();

                continue;
            }

            boolean skip = false;

            AudioFile audioFile = null;
            Tag tag;

            String name = null;
            String artist = null;
            String album = null;
            String year = null;

            try {
                try {
                    audioFile = AudioFileIO.read(song);
                    tag = audioFile.getTag();
                    name = tag.getFirst(FieldKey.TITLE);
                    artist = tag.getFirst(FieldKey.ARTIST);
                    album = tag.getFirst(FieldKey.ALBUM);
                    year = tag.getFirst(FieldKey.YEAR);
                } catch (Exception e) {
                    if (name == null) {
                        name = song.getName().split(" - ")[0];
                    }

                    if (artist == null) {
                        String[] elements = song.getName().split(" - ");

                        artist = elements[elements.length-1].split(",")[0].replace(".mp3", "");
                    }

                    if (album == null) {
                        album = "";
                    }
                }

                List<String> existingArtistNames = existingArtists.stream().map(Artist::getName).toList();
                Long artistId = 0L;

                if (existingArtistNames.contains(artist)) {
                    for (Artist ar : existingArtists) {
                        if (ar.getName().equals(artist)) {
                            artistId = ar.getId();
                            break;
                        }
                    }
                } else {
                    Artist artistToSave = new Artist();
                    artistToSave.setName(artist);

                    existingArtists.add(artistToSave);
                    artistId = artistRepo.save(artistToSave).getId();
                }

                for (SongRepo.SongSummary temp : existingSongs) {
                    if (normalize(temp.getName())
                            .equals(normalize(name)) && temp.getArtistId().equals(artistId)) {
                        PlaylistSong.PlaylistSongId linkId = new PlaylistSong.PlaylistSongId();
                        linkId.setPlaylistId(playlist.getId());
                        linkId.setSongId(temp.getId());

                        PlaylistSong link = new PlaylistSong();
                        link.setId(linkId);

                        linkBatch.add(link);

                        skip = true;
                        break;
                    }
                }

                if (skip) {
                    progress.incrementAndGet();

                    continue;
                }

                List<String> existingAlbumNames = existingAlbums.stream().map(Album::getName).toList();
                Long albumId = 0L;

                if (existingAlbumNames.contains(album)) {
                    for (Album al : existingAlbums) {
                        if (al.getName().equals(album)) {
                            albumId = al.getId();
                            break;
                        }
                    }
                } else {
                    Album albumToSave = new Album();
                    albumToSave.setName(album);
                    albumToSave.setArtistId(artistId);

                    existingAlbums.add(albumToSave);
                    albumId = albumRepo.save(albumToSave).getId();
                }

                Song songToSave = new Song();

                songToSave.setName(name);
                songToSave.setData(Files.readAllBytes(song.toPath()));
                songToSave.setArtistId(artistId);
                songToSave.setAlbumId(albumId);

                if (audioFile != null) {
                    songToSave.setLength((long) audioFile.getAudioHeader().getTrackLength());
                } else {
                    songToSave.setLength(0L);
                }

                songToSave.setPlays(0L);
                songToSave.setReleaseYear(year);
                songToSave.setNote("");
                songToSave.setLiked(false);

                songToSave = songRepo.save(songToSave);

                PlaylistSong.PlaylistSongId linkId = new PlaylistSong.PlaylistSongId();
                linkId.setPlaylistId(playlist.getId());
                linkId.setSongId(songToSave.getId());

                PlaylistSong link = new PlaylistSong();
                link.setId(linkId);

                linkBatch.add(link);

                if (linkBatch.size() == batchSize) {
                    playlistSongRepo.saveAll(linkBatch);
                    linkBatch.clear();
                }

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("This is the problem one-" + song.getName());
            }

            progress.incrementAndGet();
        }

        if (!linkBatch.isEmpty()) {
            playlistSongRepo.saveAll(linkBatch);
            linkBatch.clear();
        }

        Platform.runLater(() -> {
            importProgress.setValue(songs.length);
            done.setText("Import gotov!");
        });

        Platform.runLater(this::progressCss);
        stopProgressUpdater();
    }

    private String normalize(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFKC)
                .trim();
    }

    private void startProgressUpdater() {
        if (progressUpdater != null && !progressUpdater.isDone()) {
            progressUpdater.cancel(true);
        }

        progressUpdater = scheduler.scheduleAtFixedRate(() -> {
            Platform.runLater(this::progressCss);
        }, 0, 50, TimeUnit.MILLISECONDS);
    }

    private void stopProgressUpdater() {
        if (progressUpdater != null) {
            progressUpdater.cancel(true);
            progressUpdater = null;
        }
    }

    private void progressCss() {
        double progressPercentage = (progress.get() - importProgress.getMin())
                / (importProgress.getMax() - importProgress.getMin()) * 100;

        Platform.runLater(() -> {
            importProgress.setValue(progressPercentage);

            StackPane track = (StackPane) importProgress.lookup(".track");
            if (track != null) {
                track.setStyle(
                        String.format(
                                "-fx-padding: 4 0 4 0;" +
                                        "-fx-background-radius: 5px;" +
                                        "-fx-background-color: linear-gradient(to right, " +
                                        "purple 0%%, purple %1$.1f%%, lightgray %1$.1f%%, lightgray 100%%);",
                                progressPercentage
                        )
                );
            }
        });
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        importProgress.setDisable(true);

        Platform.runLater(() -> {
            StackPane thumb = (StackPane) importProgress.lookup(".thumb");
            if (thumb != null) {
                thumb.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
            }
        });

        navPane.getChildren().add(SetupHelper.prepareNavButton("Player"));

        navPane.getChildren().add(SetupHelper.prepareToggleButton());
    }
}
