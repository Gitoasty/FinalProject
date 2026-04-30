package com.example.finalproject.data.repository;

import com.example.finalproject.data.model.PlaylistSong;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaylistSongRepo extends JpaRepository<PlaylistSong, PlaylistSong.PlaylistSongId> {
    List<Long> findAllByIdPlaylistId(Long playlistId);
}
