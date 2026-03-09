package com.hikma.stagiaires.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "evaluations")
public class Evaluation {

    @Id
    private String id;

    private String stagiaireId;
    private String tuteurId;
    private String projetId;

    // Critères d'évaluation (notes sur 100)
    private Double qualiteTechnique;    // Pondération 40%
    private Double respectDelais;       // Pondération 20%
    private Double communication;       // Pondération 20%
    private Double espritEquipe;        // Pondération 20%

    // Score global calculé automatiquement
    // Score = (Tech x 0.40) + (Delais x 0.20) + (Comm x 0.20) + (Equipe x 0.20)
    private Double scoreGlobal;

    private String commentaire;         // Commentaire libre du tuteur

    private EvaluationStatus status;    // BROUILLON, SOUMISE, VALIDEE

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Calcul automatique du score global
    public void calculateScore() {
        if (qualiteTechnique != null && respectDelais != null
                && communication != null && espritEquipe != null) {
            this.scoreGlobal = (qualiteTechnique * 0.40)
                    + (respectDelais * 0.20)
                    + (communication * 0.20)
                    + (espritEquipe * 0.20);
        }
    }
}