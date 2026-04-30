package com.example.finalproject.data.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "song")
@Data
public class Song {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column
    private String name;
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column
    private byte[] data;
    @Column
    private Long artistId;
    @Column
    private Long albumId;
    @Column
    private Long length;
    @Column
    private Long plays;
    @Column
    private String releaseYear;
    @Column
    private String note;
    @Column
    private boolean liked;
}