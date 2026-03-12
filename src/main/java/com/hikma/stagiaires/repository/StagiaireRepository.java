package com.hikma.stagiaires.repository;

import com.hikma.stagiaires.model.Stagiaire;
import com.hikma.stagiaires.model.StagiaireStatus;
import com.hikma.stagiaires.model.EducationLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StagiaireRepository extends MongoRepository<Stagiaire, String> {

    Optional<Stagiaire> findByEmailAndDeletedFalse(String email);

    Page<Stagiaire> findByDeletedFalse(Pageable pageable);

    Page<Stagiaire> findByDepartementAndDeletedFalse(String departement, Pageable pageable);

    Page<Stagiaire> findByTuteurIdAndDeletedFalse(String tuteurId, Pageable pageable);

    List<Stagiaire> findByStatusAndDeletedFalse(StagiaireStatus status);

    long countByStatusAndDeletedFalse(StagiaireStatus status);

    long countByDepartementAndDeletedFalse(String departement);

    List<Stagiaire> findTop10ByDeletedFalseOrderByGlobalScoreDesc();

    @Query("{ 'deleted': false, 'departement': ?0 }")
    List<Stagiaire> findByDepartementForAggregation(String departement);

    boolean existsByEmailAndDeletedFalse(String email);

    // ── AJOUTÉ : pour GET /api/v1/stagiaires/me ──────────────────────────
    Optional<Stagiaire> findByUserId(String userId);
}