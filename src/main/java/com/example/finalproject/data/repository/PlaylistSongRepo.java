package com.example.finalproject.data.repository;

import com.example.finalproject.data.model.PlaylistSong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PlaylistSongRepo extends JpaRepository<PlaylistSong, PlaylistSong.PlaylistSongId> {
    List<Long> findAllByIdPlaylistId(Long playlistId);

    @Query("SELECT ps.id.songId FROM PlaylistSong ps WHERE ps.id.playlistId = :playlistId")
    List<Long> findSongIdsByPlaylistId(Long playlistId);
}
