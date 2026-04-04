package com.hikma.stagiaires.controller;

import com.hikma.stagiaires.service.TuteurRecommandationService;
import com.hikma.stagiaires.service.TuteurRecommandationService.TuteurScore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recommandations")
@RequiredArgsConstructor
@Tag(name = "Recommandations", description = "Recommandation automatique de tuteurs")
public class RecommandationController {

    private final TuteurRecommandationService recommandationService;

    // ── Recommander un tuteur pour un stagiaire précis ────────────────────
    @GetMapping("/tuteur/{stagiaireId}")
    @PreAuthorize("hasRole('RH')")
    @Operation(summary = "Top 3 tuteurs recommandés pour un stagiaire")
    public ResponseEntity<List<TuteurScore>> recommanderPourStagiaire(
            @PathVariable String stagiaireId) {
        return ResponseEntity.ok(recommandationService.recommander(stagiaireId));
    }

    // ── Liste tuteurs par charge (vue globale RH) ─────────────────────────
    @GetMapping("/tuteurs/charge")
    @PreAuthorize("hasRole('RH')")
    @Operation(summary = "Tous les tuteurs triés par disponibilité")
    public ResponseEntity<List<TuteurScore>> tuteurParCharge() {
        return ResponseEntity.ok(recommandationService.recommanderParCharge());
    }
}