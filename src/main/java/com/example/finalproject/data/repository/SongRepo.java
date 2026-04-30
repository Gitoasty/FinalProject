package com.example.finalproject.data.repository;


import com.example.finalproject.data.model.Song;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SongRepo extends JpaRepository<Song, Long> {
    interface SongSummary {
        Long getId();
        String getName();
        Long getArtistId();
    }

    List<SongSummary> findByIdIn(List<Long> ids);

    List<SongSummary> findAllProjectedBy();
}
