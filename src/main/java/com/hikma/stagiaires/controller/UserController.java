package com.hikma.stagiaires.controller;

import com.hikma.stagiaires.model.AccountStatus;
import com.hikma.stagiaires.model.Role;
import com.hikma.stagiaires.model.User;
import com.hikma.stagiaires.repository.UserRepository;
import com.hikma.stagiaires.service.AuditLogService;
import com.hikma.stagiaires.service.StagiaireService;
import com.hikma.stagiaires.dto.stagiaire.StagiaireDTOs.StagiaireResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Gestion des comptes utilisateurs")
public class UserController {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final StagiaireService stagiaireService;

    // ── Demandes en attente (RH) ──────────────────────────────────────────
    @GetMapping("/pending")
    @PreAuthorize("hasRole('RH')")
    @Operation(summary = "Liste des comptes en attente d'approbation")
    public ResponseEntity<List<UserResponse>> getPendingUsers() {
        List<UserResponse> pending = userRepository.findAll().stream()
                .filter(u -> AccountStatus.EN_ATTENTE.equals(u.getAccountStatus()))
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(pending);
    }

    // ── Tous les utilisateurs par rôle (RH) ──────────────────────────────
    @GetMapping
    @PreAuthorize("hasRole('RH')")
    @Operation(summary = "Liste tous les utilisateurs (filtre par rôle optionnel)")
    public ResponseEntity<List<UserResponse>> getAll(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {

        List<UserResponse> users = userRepository.findAll().stream()
                .filter(u -> role == null || u.getRole().name().equals(role.toUpperCase()))
                .filter(u -> status == null || u.getAccountStatus().name().equals(status.toUpperCase()))
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    // ── Approuver un compte ───────────────────────────────────────────────
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('RH')")
    @Operation(summary = "Approuver un compte utilisateur")
    public ResponseEntity<UserResponse> approveUser(
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Utilisateur introuvable : " + id));

        user.setAccountStatus(AccountStatus.APPROUVE);
        userRepository.save(user);
        auditLogService.log(currentUser.getId(), "APPROVE_USER", "USER", id, null);

        return ResponseEntity.ok(toResponse(user));
    }

    // ── Refuser un compte ─────────────────────────────────────────────────
    @PostMapping("/{id}/refuse")
    @PreAuthorize("hasRole('RH')")
    @Operation(summary = "Refuser un compte utilisateur")
    public ResponseEntity<UserResponse> refuseUser(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal User currentUser) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Utilisateur introuvable : " + id));

        user.setAccountStatus(AccountStatus.REFUSE);
        userRepository.save(user);
        auditLogService.log(currentUser.getId(), "REFUSE_USER", "USER", id, null);

        return ResponseEntity.ok(toResponse(user));
    }

    // ── Assigner un tuteur à un stagiaire (RH) ────────────────────────────
    @PutMapping("/stagiaires/{stagiaireId}/tuteur/{tuteurId}")
    @PreAuthorize("hasRole('RH')")
    @Operation(summary = "Assigner un tuteur à un stagiaire")
    public ResponseEntity<StagiaireResponse> assignTuteur(
            @PathVariable String stagiaireId,
            @PathVariable String tuteurId,
            @AuthenticationPrincipal User currentUser) {

        // Vérifie que le tuteur existe et a le bon rôle
        User tuteur = userRepository.findById(tuteurId)
                .orElseThrow(() -> new NoSuchElementException("Tuteur introuvable : " + tuteurId));

        if (!Role.TUTEUR.equals(tuteur.getRole())) {
            throw new IllegalArgumentException("Cet utilisateur n'est pas un tuteur.");
        }

        com.hikma.stagiaires.dto.stagiaire.StagiaireDTOs.UpdateRequest req =
                new com.hikma.stagiaires.dto.stagiaire.StagiaireDTOs.UpdateRequest();
        req.setTuteurId(tuteurId);

        StagiaireResponse updated = stagiaireService.update(stagiaireId, req, currentUser.getId());
        auditLogService.log(currentUser.getId(), "ASSIGN_TUTEUR", "STAGIAIRE", stagiaireId, null);

        return ResponseEntity.ok(updated);
    }

    // ── Profil de l'utilisateur connecté ─────────────────────────────────
    @GetMapping("/me")
    @Operation(summary = "Profil de l'utilisateur connecté")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(toResponse(currentUser));
    }

    // ── Liste des tuteurs approuvés (pour dropdown RH) ────────────────────
    @GetMapping("/tuteurs")
    @PreAuthorize("hasRole('RH')")
    @Operation(summary = "Liste des tuteurs approuvés")
    public ResponseEntity<List<UserResponse>> getTuteurs() {
        List<UserResponse> tuteurs = userRepository.findAll().stream()
                .filter(u -> Role.TUTEUR.equals(u.getRole())
                        && AccountStatus.APPROUVE.equals(u.getAccountStatus()))
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(tuteurs);
    }

    // ── DTO ───────────────────────────────────────────────────────────────
    @Data
    public static class UserResponse {
        private String id;
        private String firstName;
        private String lastName;
        private String email;
        private String role;
        private String accountStatus;
        private String photoUrl;
        private String createdAt;
    }

    private UserResponse toResponse(User u) {
        UserResponse r = new UserResponse();
        r.setId(u.getId());
        r.setFirstName(u.getFirstName());
        r.setLastName(u.getLastName());
        r.setEmail(u.getEmail());
        r.setRole(u.getRole().name());
        r.setAccountStatus(u.getAccountStatus() != null ? u.getAccountStatus().name() : "EN_ATTENTE");
        r.setPhotoUrl(u.getPhotoUrl());
        r.setCreatedAt(u.getCreatedAt() != null ? u.getCreatedAt().toString() : null);
        return r;
    }
}