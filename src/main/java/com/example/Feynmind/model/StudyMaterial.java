package com.example.Feynmind.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "study_materials")
@Data // Lombok: Generates Getters, Setters, etc.
@NoArgsConstructor // Lombok: Generates empty constructor
public class StudyMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;

    @Lob 
    @Column(columnDefinition = "LONGTEXT")
    private String content; 

    private LocalDateTime uploadedAt;

    public StudyMaterial(String fileName, String content) {
        this.fileName = fileName;
        this.content = content;
        this.uploadedAt = LocalDateTime.now();
    }
}