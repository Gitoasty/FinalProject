package com.example.finalproject.data.repository;

import com.example.finalproject.data.model.PlaylistSong;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlaylistSongRepo extends JpaRepository<PlaylistSong, PlaylistSong.PlaylistSongId> {
    @Query("SELECT ps.id.songId FROM PlaylistSong ps WHERE ps.id.playlistId = :playlistId")
    List<Long> findSongIdsByPlaylistId(Long playlistId);

    @Modifying
    @Transactional
    @Query("DELETE FROM PlaylistSong ps WHERE ps.id.songId = :songId")
    void deleteBySongId(@Param("songId") Long songId);
}
