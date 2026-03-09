package com.hikma.stagiaires.repository;

import com.hikma.stagiaires.model.Evaluation;
import com.hikma.stagiaires.model.EvaluationStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EvaluationRepository extends MongoRepository<Evaluation, String> {

    List<Evaluation> findByStagiaireId(String stagiaireId);

    List<Evaluation> findByTuteurId(String tuteurId);

    List<Evaluation> findByStagiaireIdAndStatus(String stagiaireId, EvaluationStatus status);

    List<Evaluation> findByProjetId(String projetId);

    long countByStatus(EvaluationStatus status);
}