package com.hikma.stagiaires.controller.Reunion;

import com.hikma.stagiaires.dto.Calendrier.CalendrierEventDTO;
import com.hikma.stagiaires.service.Reunion.CalendrierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/calendrier")
@RequiredArgsConstructor
@Tag(name = "Calendrier", description = "Événements agrégés pour le calendrier")
public class CalendrierController {

    private final CalendrierService calendrierService;

    // ── GET /api/v1/calendrier/tuteur/{id} ────────────────────────────────
    // Agrège : réunions + deadlines sprints + dates stages
    @GetMapping("/tuteur/{id}")
    @PreAuthorize("hasAnyRole('TUTEUR', 'RH')")
    @Operation(summary = "Tous les événements du calendrier tuteur")
    public ResponseEntity<List<CalendrierEventDTO>> getCalendrierTuteur(
            @PathVariable String id) {
        return ResponseEntity.ok(calendrierService.getEvenementsTuteur(id));
    }

    // ── GET /api/v1/calendrier/stagiaire/{id} ─────────────────────────────
    // Agrège : réunions + deadlines sprints + son stage
    @GetMapping("/stagiaire/{id}")
    @PreAuthorize("hasAnyRole('STAGIAIRE', 'TUTEUR', 'RH')")
    @Operation(summary = "Tous les événements du calendrier stagiaire")
    public ResponseEntity<List<CalendrierEventDTO>> getCalendrierStagiaire(
            @PathVariable String id) {
        return ResponseEntity.ok(calendrierService.getEvenementsStagiaire(id));
    }

    // ── GET /api/v1/calendrier/rh ─────────────────────────────────────────
    // Vue globale RH lecture seule
    @GetMapping("/rh")
    @PreAuthorize("hasRole('RH')")
    @Operation(summary = "Vue globale RH — tous les stages et réunions")
    public ResponseEntity<List<CalendrierEventDTO>> getCalendrierRh() {
        return ResponseEntity.ok(calendrierService.getEvenementsRh());
    }
}