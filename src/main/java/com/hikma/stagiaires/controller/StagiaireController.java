package com.hikma.stagiaires.controller;

import com.hikma.stagiaires.dto.stagiaire.StagiaireDTOs.*;
import com.hikma.stagiaires.model.User;
import com.hikma.stagiaires.service.StagiaireService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/stagiaires")
@RequiredArgsConstructor
@Tag(name = "Stagiaires", description = "Gestion des stagiaires")
public class StagiaireController {

    private final StagiaireService stagiaireService;

    @PostMapping
    @PreAuthorize("hasRole('RH')")
    @Operation(summary = "Créer un stagiaire")
    public ResponseEntity<StagiaireResponse> create(
            @Valid @RequestBody CreateRequest req,
            @AuthenticationPrincipal User currentUser) {
        StagiaireResponse resp = stagiaireService.create(req, currentUser.getId());
        return ResponseEntity.created(URI.create("/api/v1/stagiaires/" + resp.getId())).body(resp);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RH', 'TUTEUR', 'STAGIAIRE')")
    @Operation(summary = "Obtenir un stagiaire par ID")
    public ResponseEntity<StagiaireResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(stagiaireService.getById(id));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('RH', 'TUTEUR')")
    @Operation(summary = "Recherche avancée multicritères")
    public ResponseEntity<PagedResponse> search(
            @ModelAttribute SearchFilter filter,
            @AuthenticationPrincipal User currentUser) {
        // Tuteur ne voit que ses stagiaires
        if (currentUser.getRole().name().equals("TUTEUR")) {
            filter.setTuteurId(currentUser.getId());
        }
        return ResponseEntity.ok(stagiaireService.search(filter));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RH')")
    @Operation(summary = "Modifier un stagiaire")
    public ResponseEntity<StagiaireResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateRequest req,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(stagiaireService.update(id, req, currentUser.getId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('RH')")
    @Operation(summary = "Archiver un stagiaire (soft delete)")
    public ResponseEntity<Void> delete(
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        stagiaireService.softDelete(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/cv")
    @PreAuthorize("hasRole('RH')")
    @Operation(summary = "Uploader le CV (PDF)")
    public ResponseEntity<StagiaireResponse> uploadCv(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(stagiaireService.uploadCv(id, file, currentUser.getId()));
    }

    @PostMapping("/{id}/photo")
    @PreAuthorize("hasAnyRole('RH', 'STAGIAIRE')")
    @Operation(summary = "Uploader la photo de profil")
    public ResponseEntity<StagiaireResponse> uploadPhoto(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(stagiaireService.uploadPhoto(id, file, currentUser.getId()));
    }
}