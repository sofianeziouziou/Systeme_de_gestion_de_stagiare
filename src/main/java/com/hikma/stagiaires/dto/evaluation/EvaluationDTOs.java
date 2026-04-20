package com.hikma.stagiaires.dto.evaluation;

import com.hikma.stagiaires.model.evaluation.EvaluationStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

public class EvaluationDTOs {

    @Data
    public static class CreateRequest {
        @NotBlank
        private String stagiaireId;

        private String projetId;

        @NotNull @Min(0) @Max(100)
        private Double qualiteTechnique;

        @NotNull @Min(0) @Max(100)
        private Double respectDelais;

        @NotNull @Min(0) @Max(100)
        private Double communication;

        @NotNull @Min(0) @Max(100)
        private Double espritEquipe;

        private String commentaire;

        // BROUILLON (save) ou SOUMISE (submit)
        private EvaluationStatus status = EvaluationStatus.BROUILLON;
    }

    @Data
    public static class UpdateRequest {
        @Min(0) @Max(100)
        private Double qualiteTechnique;

        @Min(0) @Max(100)
        private Double respectDelais;

        @Min(0) @Max(100)
        private Double communication;

        @Min(0) @Max(100)
        private Double espritEquipe;

        private String commentaire;
        private EvaluationStatus status;
    }

    @Data
    public static class EvaluationResponse {
        private String id;
        private String stagiaireId;
        private String stagiaireFullName;
        private String tuteurId;
        private String tuteurFullName;
        private String projetId;
        private String projetTitle;
        private Double qualiteTechnique;
        private Double respectDelais;
        private Double communication;
        private Double espritEquipe;
        private Double scoreGlobal;
        private String commentaire;
        private EvaluationStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    public static class ScoreBreakdown {
        private Double qualiteTechnique;
        private Double respectDelais;
        private Double communication;
        private Double espritEquipe;
        private Double scoreGlobal;
        private String badge;
    }
}