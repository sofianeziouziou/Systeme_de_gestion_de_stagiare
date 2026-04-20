package com.hikma.stagiaires.repository;

import com.hikma.stagiaires.model.reunion.Reunion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReunionRepository extends MongoRepository<Reunion, String> {

    List<Reunion> findByTuteurId(String tuteurId);

    List<Reunion> findByStagiaireIdsContaining(String stagiaireId);

    List<Reunion> findByTuteurIdAndDateHeureBetween(
            String tuteurId,
            LocalDateTime debut,
            LocalDateTime fin
    );

    List<Reunion> findByStagiaireIdsContainingAndDateHeureBetween(
            String stagiaireId,
            LocalDateTime debut,
            LocalDateTime fin
    );
}