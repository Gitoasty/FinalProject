package com.example.finalproject.data.repository;

import com.example.finalproject.data.model.Album;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlbumRepo extends JpaRepository<Album, Long> {
    Long findIdByName(String name);
}
