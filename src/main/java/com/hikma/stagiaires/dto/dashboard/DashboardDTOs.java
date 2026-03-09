package com.hikma.stagiaires.dto.dashboard;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

public class DashboardDTOs {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DashboardStats {
        // KPIs principaux
        private long totalStagiairesActifs;
        private long totalStagiairesTermines;
        private long totalStagiairesEnRetard;
        private long totalProjets;
        private long projetsEnCours;
        private long projetsTermines;
        private double scoreGlobalMoyen;

        // Top 10 des meilleurs stagiaires
        private List<TopStagiaireDTO> top10Stagiaires;

        // Score moyen par département
        private Map<String, Double> scoreMoyenParDepartement;

        // Taux de completion par département
        private Map<String, Double> tauxCompletionParDepartement;

        // Distribution des scores (pour histogramme)
        private List<ScoreDistributionDTO> scoreDistribution;

        // Performances par critère (pour radar chart)
        private CriteresPerformance moyennesCriteres;

        // Evolution mensuelle
        private List<MonthlyStatsDTO> evolutionMensuelle;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TopStagiaireDTO {
        private String id;
        private String firstName;
        private String lastName;
        private String photoUrl;
        private String departement;
        private Double score;
        private String badge;
        private int rank;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ScoreDistributionDTO {
        private String range;   // ex: "0-20", "21-40"...
        private long count;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CriteresPerformance {
        private double qualiteTechnique;
        private double respectDelais;
        private double communication;
        private double espritEquipe;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MonthlyStatsDTO {
        private String month;       // ex: "2025-01"
        private double avgScore;
        private long newStagiaires;
        private long completedProjects;
    }
}