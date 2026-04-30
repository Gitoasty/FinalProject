package com.example.finalproject.data.model;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;

@Entity
@Table(name = "playlist_song")
@Data
public class PlaylistSong {
    @EmbeddedId
    private PlaylistSongId id = new PlaylistSongId();

    @Embeddable
    @Data
    public static class PlaylistSongId implements Serializable {
        private Long playlistId;
        private Long songId;
    }
}
