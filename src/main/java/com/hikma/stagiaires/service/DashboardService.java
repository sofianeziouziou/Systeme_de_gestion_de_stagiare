package com.hikma.stagiaires.service;

import com.hikma.stagiaires.dto.dashboard.DashboardDTOs.*;
import com.hikma.stagiaires.model.*;
import com.hikma.stagiaires.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final StagiaireRepository stagiaireRepository;
    private final ProjetRepository projetRepository;
    private final EvaluationRepository evaluationRepository;
    private final StagiaireService stagiaireService;

    @Cacheable(value = "dashboard", key = "'global'")
    public DashboardStats getGlobalStats() {

        // KPIs principaux
        long actifs = stagiaireRepository.countByStatusAndDeletedFalse(StagiaireStatus.EN_COURS);
        long termines = stagiaireRepository.countByStatusAndDeletedFalse(StagiaireStatus.TERMINE);
        long enRetard = projetRepository.countByStatusAndDeletedFalse(ProjetStatus.EN_RETARD);
        long totalProjets = projetRepository.countByStatusAndDeletedFalse(ProjetStatus.EN_COURS)
                + projetRepository.countByStatusAndDeletedFalse(ProjetStatus.TERMINE);

        // Score moyen global
        List<Stagiaire> allActive = stagiaireRepository.findByStatusAndDeletedFalse(StagiaireStatus.EN_COURS);
        double scoreMoyen = allActive.stream()
                .mapToDouble(s -> s.getGlobalScore() != null ? s.getGlobalScore() : 0)
                .average().orElse(0);

        // Top 10
        List<TopStagiaireDTO> top10 = stagiaireRepository
                .findTop10ByDeletedFalseOrderByGlobalScoreDesc()
                .stream().map((Stagiaire s) -> TopStagiaireDTO.builder()
                        .id(s.getId())
                        .firstName(s.getFirstName())
                        .lastName(s.getLastName())
                        .photoUrl(s.getPhotoUrl())
                        .departement(s.getDepartement())
                        .score(s.getGlobalScore())
                        .badge(s.getBadge() != null ? s.getBadge().name() : null)
                        .build())
                .collect(Collectors.toList());

        // Rang
        for (int i = 0; i < top10.size(); i++) top10.get(i).setRank(i + 1);

        // Score moyen par département
        List<String> depts = List.of("IT", "Finance", "Marketing", "Production", "Qualite");
        Map<String, Double> scoreParDept = new LinkedHashMap<>();
        for (String dept : depts) {
            List<Stagiaire> stagsInDept = stagiaireRepository.findByDepartementForAggregation(dept);
            double avg = stagsInDept.stream()
                    .mapToDouble(s -> s.getGlobalScore() != null ? s.getGlobalScore() : 0)
                    .average().orElse(0);
            scoreParDept.put(dept, Math.round(avg * 100.0) / 100.0);
        }

        // Distribution des scores
        List<Stagiaire> allStagiaires = stagiaireRepository.findByDeletedFalse(
                org.springframework.data.domain.Pageable.unpaged()).getContent();

        Map<String, Long> distrib = new LinkedHashMap<>();
        distrib.put("0-20", allStagiaires.stream().filter(s -> s.getGlobalScore() != null && s.getGlobalScore() <= 20).count());
        distrib.put("21-40", allStagiaires.stream().filter(s -> s.getGlobalScore() != null && s.getGlobalScore() > 20 && s.getGlobalScore() <= 40).count());
        distrib.put("41-60", allStagiaires.stream().filter(s -> s.getGlobalScore() != null && s.getGlobalScore() > 40 && s.getGlobalScore() <= 60).count());
        distrib.put("61-75", allStagiaires.stream().filter(s -> s.getGlobalScore() != null && s.getGlobalScore() > 60 && s.getGlobalScore() <= 75).count());
        distrib.put("76-89", allStagiaires.stream().filter(s -> s.getGlobalScore() != null && s.getGlobalScore() > 75 && s.getGlobalScore() <= 89).count());
        distrib.put("90-100", allStagiaires.stream().filter(s -> s.getGlobalScore() != null && s.getGlobalScore() >= 90).count());

        List<ScoreDistributionDTO> scoreDistrib = distrib.entrySet().stream()
                .map(e -> ScoreDistributionDTO.builder().range(e.getKey()).count(e.getValue()).build())
                .collect(Collectors.toList());

        // Moyennes par critère (pour radar chart)
        List<Evaluation> allEvals = evaluationRepository.findByStatus(EvaluationStatus.VALIDEE);
        double avgTech = allEvals.stream().mapToDouble(Evaluation::getQualiteTechnique).average().orElse(0);
        double avgDelais = allEvals.stream().mapToDouble(Evaluation::getRespectDelais).average().orElse(0);
        double avgComm = allEvals.stream().mapToDouble(Evaluation::getCommunication).average().orElse(0);
        double avgEquipe = allEvals.stream().mapToDouble(Evaluation::getEspritEquipe).average().orElse(0);

        CriteresPerformance criteres = CriteresPerformance.builder()
                .qualiteTechnique(avgTech)
                .respectDelais(avgDelais)
                .communication(avgComm)
                .espritEquipe(avgEquipe)
                .build();

        return DashboardStats.builder()
                .totalStagiairesActifs(actifs)
                .totalStagiairesTermines(termines)
                .totalStagiairesEnRetard(enRetard)
                .totalProjets(totalProjets)
                .projetsEnCours(projetRepository.countByStatusAndDeletedFalse(ProjetStatus.EN_COURS))
                .projetsTermines(projetRepository.countByStatusAndDeletedFalse(ProjetStatus.TERMINE))
                .scoreGlobalMoyen(Math.round(scoreMoyen * 100.0) / 100.0)
                .top10Stagiaires(top10)
                .scoreMoyenParDepartement(scoreParDept)
                .scoreDistribution(scoreDistrib)
                .moyennesCriteres(criteres)
                .build();
    }
}