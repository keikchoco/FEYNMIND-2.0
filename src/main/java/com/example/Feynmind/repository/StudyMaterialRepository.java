package com.example.Feynmind.repository;

import com.example.Feynmind.model.StudyMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudyMaterialRepository extends JpaRepository<StudyMaterial, Long> {
    
}