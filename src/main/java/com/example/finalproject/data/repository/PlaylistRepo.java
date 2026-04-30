package com.example.finalproject.data.repository;

import com.example.finalproject.data.model.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlaylistRepo extends JpaRepository<Playlist, Long> {
    Optional<Playlist> findByName(String name);
}
