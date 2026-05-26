package com.example.finalproject.controllers;

import com.example.finalproject.data.model.*;
import com.example.finalproject.data.repository.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.DirectoryChooser;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.text.Normalizer;
import java.util.*;

@Component
public class ImportController implements Initializable {
    @FXML
    private Button chooseButton;
    @FXML
    private Label playlistName;
    @FXML
    private ProgressBar importProgress;

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

    public void choose(ActionEvent e) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select playlist folder");

        selectedDirectory = directoryChooser.showDialog(chooseButton.getScene().getWindow());

        if (selectedDirectory != null) {
            playlistName.setText(selectedDirectory.getName());
        }
    }

    public void importPlaylist() {
        if (selectedDirectory != null) {
            List<File> songs = Arrays.asList(selectedDirectory.listFiles());

            int progress = 0;
            importProgress.setProgress(progress);

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
                boolean skip = false;

                AudioFile audioFile = null;
                Tag tag = null;

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

            }

            if (!linkBatch.isEmpty()) {
                playlistSongRepo.saveAll(linkBatch);
                linkBatch.clear();
            }
        }
    }

    private String normalize(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFKC)
                .trim();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }
}
