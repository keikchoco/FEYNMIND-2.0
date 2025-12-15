package com.example.Feynmind.repository;

import com.example.Feynmind.model.StudyMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List; // <--- Make sure this is imported

@Repository
public interface StudyMaterialRepository extends JpaRepository<StudyMaterial, Long> {
    // CHANGE: Return a List instead of Optional
    List<StudyMaterial> findByFileName(String fileName);
}