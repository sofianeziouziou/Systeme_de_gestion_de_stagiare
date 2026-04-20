package com.hikma.stagiaires.dto.Calendrier;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendrierEventDTO {

    private String id;
    private String title;
    private LocalDateTime start;
    private LocalDateTime end;
    private String type;        // "REUNION", "SPRINT_DEADLINE", "STAGE_DEBUT", "STAGE_FIN"
    private String color;       // "#3B82F6", "#F59E0B", "#EF4444", "#10B981"
    private String projetId;
    private String stagiaireId;
    private boolean editable;
}