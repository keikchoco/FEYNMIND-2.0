package com.example.Feynmind.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "study_materials")
public class StudyMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;

    // Large content field to store study material data
    @Lob 
    @Column(columnDefinition = "LONGTEXT")
    private String content; 

    private LocalDateTime uploadedAt;

    // Constructors
    public StudyMaterial() {}

    public StudyMaterial(String fileName, String content) {
        this.fileName = fileName;
        this.content = content;
        this.uploadedAt = LocalDateTime.now();
    }

    // Get
    public Long getId() { return id; }
    public String getFileName() { return fileName; }
    public String getContent() { return content; }
}