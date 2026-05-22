package com.example.finalproject.data.repository;


import com.example.finalproject.data.model.Song;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query(value = "SELECT s.data FROM Song s WHERE s.id = :id")
    byte[] findDataById(@Param("id") Long id);

//    @Query("SELECT new com.example.finalproject.data.SongDataDTO(s.data) FROM Song s WHERE s.id = :id")
//    SongDataDTO findDataById(@Param("id") Long id);
}
