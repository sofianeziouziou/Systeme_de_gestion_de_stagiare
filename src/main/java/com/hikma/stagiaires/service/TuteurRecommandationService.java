package com.hikma.stagiaires.service;

import com.hikma.stagiaires.dto.stagiaire.StagiaireDTOs.StagiaireResponse;
import com.hikma.stagiaires.model.*;
import com.hikma.stagiaires.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TuteurRecommandationService {

    private final UserRepository       userRepository;
    private final StagiaireRepository  stagiaireRepository;

    // ── DTO résultat ──────────────────────────────────────────────────────
    public record TuteurScore(
            String  tuteurId,
            String  firstName,
            String  lastName,
            String  email,
            double  score,
            int     nbStagiaires,
            double  scoreMoyenStagiaires,
            String  raison
    ) {}

    // ── Algorithme principal ──────────────────────────────────────────────
    /**
     * Recommande les 3 meilleurs tuteurs pour un stagiaire donné.
     *
     * Critères pondérés :
     *   40% — Charge (moins de stagiaires = meilleur)
     *   30% — Historique (score moyen de ses stagiaires passés)
     *   20% — Département (même département que le stagiaire)
     *   10% — Compétences (overlap entre technicalSkills du tuteur et du stagiaire)
     */
    public List<TuteurScore> recommander(String stagiaireId) {
        // 1. Charger le stagiaire cible
        Stagiaire stagiaire = stagiaireRepository.findById(stagiaireId)
                .orElseThrow(() -> new NoSuchElementException("Stagiaire introuvable : " + stagiaireId));

        // 2. Charger tous les tuteurs actifs
        List<User> tuteurs = userRepository.findAll().stream()
                .filter(u -> Role.TUTEUR.equals(u.getRole()))
                .filter(u -> AccountStatus.APPROUVE.equals(u.getAccountStatus()))
                .collect(Collectors.toList());

        if (tuteurs.isEmpty()) return List.of();

        // 3. Pour chaque tuteur, calculer le score
        List<TuteurScore> scores = new ArrayList<>();

        for (User tuteur : tuteurs) {
            // Stagiaires actuels de ce tuteur (actifs)
            List<Stagiaire> stagiairesDuTuteur = stagiaireRepository
                    .findByTuteurIdAndDeletedFalse(tuteur.getId());

            int    nbActuels    = stagiairesDuTuteur.size();
            double scoreMoyen   = stagiairesDuTuteur.stream()
                    .mapToDouble(s -> s.getGlobalScore() != null ? s.getGlobalScore() : 0)
                    .average().orElse(0.0);

            // ── Critère 1 : Charge (40%) ──────────────────────────────────
            // Score max si 0 stagiaire, diminue avec la charge (max raisonnable = 5)
            double scoreCharge = Math.max(0, 1.0 - (nbActuels / 5.0)) * 40;

            // ── Critère 2 : Historique performances (30%) ─────────────────
            double scoreHistorique = (scoreMoyen / 100.0) * 30;

            // ── Critère 3 : Département (20%) ─────────────────────────────
            double scoreDept = 0;
            if (stagiaire.getDepartement() != null) {
                // Vérifier si le tuteur a déjà encadré dans ce département
                boolean mêmeDept = stagiairesDuTuteur.stream()
                        .anyMatch(s -> stagiaire.getDepartement().equalsIgnoreCase(s.getDepartement()));
                scoreDept = mêmeDept ? 20 : 0;
            }

            // ── Critère 4 : Compétences (10%) ─────────────────────────────
            double scoreCompetences = 0;
            List<String> skillsStagiaire = stagiaire.getTechnicalSkills();
            if (skillsStagiaire != null && !skillsStagiaire.isEmpty()) {
                // Récupérer les compétences du tuteur via ses stagiaires précédents
                Set<String> skillsTuteur = stagiairesDuTuteur.stream()
                        .filter(s -> s.getTechnicalSkills() != null)
                        .flatMap(s -> s.getTechnicalSkills().stream())
                        .map(String::toLowerCase)
                        .collect(Collectors.toSet());

                long overlap = skillsStagiaire.stream()
                        .map(String::toLowerCase)
                        .filter(skillsTuteur::contains)
                        .count();

                scoreCompetences = skillsStagiaire.isEmpty() ? 0
                        : ((double) overlap / skillsStagiaire.size()) * 10;
            }

            double scoreTotal = scoreCharge + scoreHistorique + scoreDept + scoreCompetences;

            // ── Construire la raison ───────────────────────────────────────
            String raison = buildRaison(nbActuels, scoreMoyen, scoreDept > 0, scoreCompetences);

            scores.add(new TuteurScore(
                    tuteur.getId(),
                    tuteur.getFirstName(),
                    tuteur.getLastName(),
                    tuteur.getEmail(),
                    Math.round(scoreTotal * 10.0) / 10.0,
                    nbActuels,
                    Math.round(scoreMoyen * 10.0) / 10.0,
                    raison
            ));
        }

        // Trier par score décroissant, retourner top 3
        return scores.stream()
                .sorted(Comparator.comparingDouble(TuteurScore::score).reversed())
                .limit(3)
                .collect(Collectors.toList());
    }

    // ── Recommandation rapide sans stagiaireId (juste équilibrer charges) ─
    public List<TuteurScore> recommanderParCharge() {
        List<User> tuteurs = userRepository.findAll().stream()
                .filter(u -> Role.TUTEUR.equals(u.getRole()))
                .filter(u -> AccountStatus.APPROUVE.equals(u.getAccountStatus()))
                .collect(Collectors.toList());

        return tuteurs.stream().map(tuteur -> {
                    List<Stagiaire> stagiaires = stagiaireRepository
                            .findByTuteurIdAndDeletedFalse(tuteur.getId());
                    int    nb    = stagiaires.size();
                    double score = stagiaires.stream()
                            .mapToDouble(s -> s.getGlobalScore() != null ? s.getGlobalScore() : 0)
                            .average().orElse(0.0);
                    double charge = Math.max(0, 1.0 - (nb / 5.0)) * 100;
                    return new TuteurScore(
                            tuteur.getId(), tuteur.getFirstName(), tuteur.getLastName(),
                            tuteur.getEmail(), charge, nb, score,
                            nb == 0 ? "Disponible — aucun stagiaire actuellement"
                                    : nb < 3 ? "Charge légère (" + nb + " stagiaire(s))"
                                    : "Charge élevée (" + nb + " stagiaires)"
                    );
                })
                .sorted(Comparator.comparingDouble(TuteurScore::score).reversed())
                .collect(Collectors.toList());
    }

    private String buildRaison(int nb, double scoreMoyen, boolean mêmeDept, double scoreComp) {
        List<String> points = new ArrayList<>();
        if (nb == 0)       points.add("disponible (0 stagiaire)");
        else if (nb <= 2)  points.add("charge légère (" + nb + " stagiaires)");
        else               points.add("charge élevée (" + nb + " stagiaires)");
        if (scoreMoyen >= 75) points.add("excellent historique (" + Math.round(scoreMoyen) + "/100)");
        else if (scoreMoyen >= 50) points.add("bon historique (" + Math.round(scoreMoyen) + "/100)");
        if (mêmeDept)      points.add("même département");
        if (scoreComp > 0) points.add("compétences compatibles");
        return String.join(" · ", points);
    }
}