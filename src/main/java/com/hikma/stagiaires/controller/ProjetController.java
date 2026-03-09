package com.hikma.stagiaires.controller;

import com.hikma.stagiaires.dto.projet.ProjetDTOs.*;
import com.hikma.stagiaires.model.User;
import com.hikma.stagiaires.service.ProjetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/projets")
@RequiredArgsConstructor
@Tag(name = "Projets", description = "Gestion des projets de stage")
public class ProjetController {

    private final ProjetService projetService;

    @PostMapping
    @PreAuthorize("hasAnyRole('RH', 'TUTEUR')")
    @Operation(summary = "Créer un projet")
    public ResponseEntity<ProjetResponse> create(
            @Valid @RequestBody CreateRequest req,
            @AuthenticationPrincipal User currentUser) {
        ProjetResponse resp = projetService.create(req, currentUser.getId());
        return ResponseEntity.created(URI.create("/api/v1/projets/" + resp.getId())).body(resp);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('RH', 'TUTEUR')")
    @Operation(summary = "Lister tous les projets (paginé)")
    public ResponseEntity<Page<ProjetResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(projetService.getAll(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RH', 'TUTEUR', 'STAGIAIRE')")
    @Operation(summary = "Obtenir un projet par ID")
    public ResponseEntity<ProjetResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(projetService.getById(id));
    }

    @GetMapping("/my-projects")
    @PreAuthorize("hasRole('TUTEUR')")
    @Operation(summary = "Mes projets (tuteur)")
    public ResponseEntity<List<ProjetResponse>> getMyProjects(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(projetService.getByTuteur(currentUser.getId()));
    }

    @GetMapping("/stagiaire/{stagiaireId}")
    @PreAuthorize("hasAnyRole('RH', 'TUTEUR', 'STAGIAIRE')")
    @Operation(summary = "Projets d'un stagiaire")
    public ResponseEntity<List<ProjetResponse>> getByStagiaire(@PathVariable String stagiaireId) {
        return ResponseEntity.ok(projetService.getByStagiaire(stagiaireId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RH', 'TUTEUR')")
    @Operation(summary = "Modifier un projet")
    public ResponseEntity<ProjetResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateRequest req,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(projetService.update(id, req, currentUser.getId()));
    }

    @PatchMapping("/{id}/progress")
    @PreAuthorize("hasAnyRole('RH', 'TUTEUR')")
    @Operation(summary = "Mettre à jour l'avancement")
    public ResponseEntity<ProjetResponse> updateProgress(
            @PathVariable String id,
            @RequestParam Integer progress,
            @AuthenticationPrincipal User currentUser) {
        UpdateRequest req = new UpdateRequest();
        req.setProgress(progress);
        return ResponseEntity.ok(projetService.update(id, req, currentUser.getId()));
    }

    @PostMapping("/{id}/report")
    @PreAuthorize("hasAnyRole('RH', 'TUTEUR', 'STAGIAIRE')")
    @Operation(summary = "Déposer le rapport final (PDF)")
    public ResponseEntity<ProjetResponse> uploadReport(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(projetService.uploadReport(id, file, currentUser.getId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('RH')")
    @Operation(summary = "Archiver un projet")
    public ResponseEntity<Void> delete(
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        projetService.softDelete(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}