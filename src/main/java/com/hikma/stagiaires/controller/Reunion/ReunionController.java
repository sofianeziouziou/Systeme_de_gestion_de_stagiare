package com.hikma.stagiaires.controller.Reunion;

import com.hikma.stagiaires.model.user.User;
import com.hikma.stagiaires.model.reunion.Reunion;
import com.hikma.stagiaires.service.Reunion.ReunionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reunions")
@RequiredArgsConstructor
@Tag(name = "Réunions", description = "Gestion du calendrier et des réunions")
public class ReunionController {

    private final ReunionService reunionService;

    // ── POST /api/v1/reunions ─────────────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasRole('TUTEUR')")
    @Operation(summary = "Tuteur crée une réunion")
    public ResponseEntity<Reunion> creer(
            @RequestBody ReunionRequest req,
            @AuthenticationPrincipal User tuteur) {

        Reunion reunion = Reunion.builder()
                .tuteurId(tuteur.getId())
                .tuteurName(tuteur.getFirstName() + " " + tuteur.getLastName())
                .stagiaireIds(req.getStagiaireIds())
                .stagiaireNames(req.getStagiaireNames())
                .sujet(req.getSujet())
                .dateHeure(req.getDateHeure())
                .dureeMins(req.getDureeMins() > 0 ? req.getDureeMins() : 60)
                .lieu(req.getLieu())
                .notes(req.getNotes())
                .build();

        return ResponseEntity.ok(reunionService.creer(reunion));
    }

    // ── GET /api/v1/reunions/tuteur/{tuteurId} ────────────────────────────
    @GetMapping("/tuteur/{tuteurId}")
    @PreAuthorize("hasAnyRole('TUTEUR', 'RH')")
    @Operation(summary = "Réunions d'un tuteur")
    public ResponseEntity<List<Reunion>> getByTuteur(
            @PathVariable String tuteurId) {
        return ResponseEntity.ok(reunionService.getByTuteur(tuteurId));
    }

    // ── GET /api/v1/reunions/stagiaire/{stagiaireId} ──────────────────────
    @GetMapping("/stagiaire/{stagiaireId}")
    @PreAuthorize("hasAnyRole('STAGIAIRE', 'TUTEUR', 'RH')")
    @Operation(summary = "Réunions d'un stagiaire")
    public ResponseEntity<List<Reunion>> getByStagiaire(
            @PathVariable String stagiaireId) {
        return ResponseEntity.ok(reunionService.getByStagiaire(stagiaireId));
    }

    // ── PUT /api/v1/reunions/{id} ─────────────────────────────────────────
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TUTEUR')")
    @Operation(summary = "Modifier une réunion")
    public ResponseEntity<Reunion> modifier(
            @PathVariable String id,
            @RequestBody ReunionRequest req) {

        Reunion update = Reunion.builder()
                .sujet(req.getSujet())
                .dateHeure(req.getDateHeure())
                .dureeMins(req.getDureeMins())
                .lieu(req.getLieu())
                .notes(req.getNotes())
                .stagiaireIds(req.getStagiaireIds())
                .stagiaireNames(req.getStagiaireNames())
                .build();

        return ResponseEntity.ok(reunionService.modifier(id, update));
    }

    // ── DELETE /api/v1/reunions/{id} ──────────────────────────────────────
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TUTEUR', 'RH')")
    @Operation(summary = "Supprimer une réunion")
    public ResponseEntity<Void> supprimer(@PathVariable String id) {
        reunionService.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    // ── PATCH /api/v1/reunions/{id}/confirmer ─────────────────────────────
    @PatchMapping("/{id}/confirmer")
    @PreAuthorize("hasRole('STAGIAIRE')")
    @Operation(summary = "Stagiaire confirme une réunion")
    public ResponseEntity<Reunion> confirmer(@PathVariable String id) {
        return ResponseEntity.ok(reunionService.confirmer(id));
    }

    // ── PATCH /api/v1/reunions/{id}/decliner ──────────────────────────────
    @PatchMapping("/{id}/decliner")
    @PreAuthorize("hasRole('STAGIAIRE')")
    @Operation(summary = "Stagiaire décline une réunion")
    public ResponseEntity<Reunion> decliner(@PathVariable String id) {
        return ResponseEntity.ok(reunionService.decliner(id));
    }

    // ── DTO ───────────────────────────────────────────────────────────────
    @Data
    public static class ReunionRequest {
        private List<String> stagiaireIds;
        private List<String> stagiaireNames;
        private String sujet;
        private LocalDateTime dateHeure;
        private int dureeMins;
        private String lieu;
        private String notes;
    }
}