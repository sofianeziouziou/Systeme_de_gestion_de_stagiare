package com.hikma.stagiaires.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "projets")
public class Projet {

    @Id
    private String id;

    private String title;           // max 100 caractères
    private String description;     // contexte et objectifs (rich text)

    private List<String> stagiaireIds;   // 1 à N stagiaires par projet
    private String tuteurId;             // lié automatiquement au tuteur créateur

    // Dates
    private LocalDate startDate;
    private LocalDate plannedEndDate;
    private LocalDate actualEndDate;

    // Avancement
    @Builder.Default
    private Integer progress = 0;        // 0-100%

    @Builder.Default
    private List<Jalon> jalons = List.of();

    // Statut
    @Builder.Default
    private ProjetStatus status = ProjetStatus.EN_COURS;

    // Technologies utilisées (tags)
    @Builder.Default
    private List<String> technologies = List.of();

    // Rapport final
    private String reportUrl;
    private LocalDate reportSubmittedAt;

    @Builder.Default
    private boolean deleted = false;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Classe interne Jalon
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Jalon {
        private String title;
        private LocalDate date;
        private boolean completed;
    }
}