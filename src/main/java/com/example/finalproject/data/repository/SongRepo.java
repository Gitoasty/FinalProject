package com.example.finalproject.data.repository;


import com.example.finalproject.data.model.Song;
import jakarta.transaction.Transactional;
import javafx.beans.value.ObservableValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SongRepo extends JpaRepository<Song, Long> {
    interface SongSummary {
        Long getId();
        Long getLength();
        String getName();
        Long getArtistId();
    }

    interface SongInfo {
        Long getId();
        String getName();
        Long getArtistId();
        Long getAlbumId();
        Long getLength();
        Long getPlays();
        String getReleaseYear();
        String getNote();
        Boolean getLiked();
    }

    List<SongInfo> findAllSongInfoProjectedBy();

    List<SongSummary> findByIdIn(List<Long> ids);
    List<SongSummary> findAllProjectedBy();

    @Query(value = "SELECT s.data FROM Song s WHERE s.id = :id")
    byte[] findDataById(@Param("id") Long id);

    @Query(value = "SELECT s.plays FROM Song s WHERE s.id = :id")
    Long findPlaysById(Long id);

    @Modifying
    @Query("UPDATE Song s SET s.plays = :value WHERE s.id = :id")
    @Transactional
    void updatePlays(@Param("id") Long id, @Param("value") Long value);
}
