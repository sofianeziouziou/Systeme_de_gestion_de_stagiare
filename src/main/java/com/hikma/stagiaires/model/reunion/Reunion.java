package com.hikma.stagiaires.model.reunion;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "reunions")
public class Reunion {

    @Id
    private String id;

    private String tuteurId;
    private String tuteurName;

    private List<String> stagiaireIds;
    private List<String> stagiaireNames;

    private String sujet;
    private LocalDateTime dateHeure;

    @Builder.Default
    private int dureeMins = 60; // 30, 60, 90

    private String lieu; // "Teams", "Présentiel", "Zoom"

    @Builder.Default
    private String statut = "PLANIFIEE"; // "PLANIFIEE", "CONFIRMEE", "ANNULEE"

    private String notes;

    @CreatedDate
    private LocalDateTime createdAt;
}