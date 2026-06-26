package com.example.finalproject.data;

import com.example.finalproject.data.model.SongDto;
import com.example.finalproject.data.repository.AlbumRepo;
import com.example.finalproject.data.repository.ArtistRepo;
import com.example.finalproject.data.repository.SongRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UtilService {

    private final SongRepo songRepo;
    private final ArtistRepo artistRepo;
    private final AlbumRepo albumRepo;

    public String getArtist(Long id) {
        return artistRepo.findById(id).get().getName();
    }

    public String getAlbum(Long id) {
        return albumRepo.findById(id).get().getName();
    }
}
