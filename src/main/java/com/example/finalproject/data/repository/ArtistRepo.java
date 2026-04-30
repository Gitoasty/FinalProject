package com.example.finalproject.data.repository;

import com.example.finalproject.data.model.Artist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistRepo extends JpaRepository<Artist, Long> {
    Long findIdByName(String name);
}
