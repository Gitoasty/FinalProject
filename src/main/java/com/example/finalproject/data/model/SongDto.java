package com.example.finalproject.data.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SongDto {
    private Long id;
    private String name;
    private Long artistId;
    private Long albumId;
    private Long length;
    private Long plays;
    private String releaseYear;
    private String note;
    private Boolean liked;
}
