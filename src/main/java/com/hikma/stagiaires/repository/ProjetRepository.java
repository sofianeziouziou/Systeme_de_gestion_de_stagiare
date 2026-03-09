package com.hikma.stagiaires.repository;

import com.hikma.stagiaires.model.Projet;
import com.hikma.stagiaires.model.ProjetStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface ProjetRepository extends MongoRepository<Projet, String> {

    Page<Projet> findByDeletedFalse(Pageable pageable);

    List<Projet> findByTuteurIdAndDeletedFalse(String tuteurId);

    List<Projet> findByStagiaireIdsContainingAndDeletedFalse(String stagiaireId);

    List<Projet> findByStatusAndDeletedFalse(ProjetStatus status);

    long countByStatusAndDeletedFalse(ProjetStatus status);

    // Projets dont la deadline approche (dans les 7 prochains jours)
    @Query("{ 'deleted': false, 'status': 'EN_COURS', 'plannedEndDate': { $gte: ?0, $lte: ?1 } }")
    List<Projet> findProjetsByDeadlineApproching(LocalDate now, LocalDate limit);

    // Projets en retard (deadline dépassée et pas terminé)
    @Query("{ 'deleted': false, 'status': { $ne: 'TERMINE' }, 'plannedEndDate': { $lt: ?0 } }")
    List<Projet> findOverdueProjects(LocalDate now);

    // Projets sans mise à jour depuis 5 jours
    @Query("{ 'deleted': false, 'status': 'EN_COURS', 'updatedAt': { $lt: ?0 } }")
    List<Projet> findProjectsWithoutRecentUpdate(java.time.LocalDateTime limit);
}