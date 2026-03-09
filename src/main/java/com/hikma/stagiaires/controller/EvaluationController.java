package com.hikma.stagiaires.controller;

import com.hikma.stagiaires.dto.evaluation.EvaluationDTOs.*;
import com.hikma.stagiaires.model.User;
import com.hikma.stagiaires.service.EvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/evaluations")
@RequiredArgsConstructor
@Tag(name = "Evaluations", description = "Évaluations multicritères des stagiaires")
public class EvaluationController {

    private final EvaluationService evaluationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('RH', 'TUTEUR')")
    @Operation(summary = "Créer une évaluation")
    public ResponseEntity<EvaluationResponse> create(
            @Valid @RequestBody CreateRequest req,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(evaluationService.create(req, currentUser.getId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RH', 'TUTEUR')")
    @Operation(summary = "Modifier une évaluation")
    public ResponseEntity<EvaluationResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateRequest req,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(evaluationService.update(id, req, currentUser.getId()));
    }

    @PostMapping("/{id}/validate")
    @PreAuthorize("hasRole('RH')")
    @Operation(summary = "Valider une évaluation (RH uniquement)")
    public ResponseEntity<EvaluationResponse> validate(
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(evaluationService.validate(id, currentUser.getId()));
    }

    @GetMapping("/stagiaire/{stagiaireId}")
    @PreAuthorize("hasAnyRole('RH', 'TUTEUR', 'STAGIAIRE')")
    @Operation(summary = "Évaluations d'un stagiaire")
    public ResponseEntity<List<EvaluationResponse>> getByStagiaire(@PathVariable String stagiaireId) {
        return ResponseEntity.ok(evaluationService.getByStagiaire(stagiaireId));
    }

    @GetMapping("/my-evaluations")
    @PreAuthorize("hasRole('TUTEUR')")
    @Operation(summary = "Mes évaluations (tuteur)")
    public ResponseEntity<List<EvaluationResponse>> getMyEvaluations(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(evaluationService.getByTuteur(currentUser.getId()));
    }

    @GetMapping("/stagiaire/{stagiaireId}/score")
    @PreAuthorize("hasAnyRole('RH', 'TUTEUR', 'STAGIAIRE')")
    @Operation(summary = "Détail du score d'un stagiaire")
    public ResponseEntity<ScoreBreakdown> getScoreBreakdown(@PathVariable String stagiaireId) {
        return ResponseEntity.ok(evaluationService.getScoreBreakdown(stagiaireId));
    }
}