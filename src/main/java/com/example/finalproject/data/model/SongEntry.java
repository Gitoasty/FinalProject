package com.example.finalproject.data.model;

import com.example.finalproject.data.repository.SongRepo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SongEntry {
    private Long id;
    private Long length;
    private String name;

    public SongEntry(SongRepo.SongSummary s) {
        this.id = s.getId();
        this.length = s.getLength();
        this.name = s.getName();
    }

    @Override
    public String toString() {
        return name;
    }
}
