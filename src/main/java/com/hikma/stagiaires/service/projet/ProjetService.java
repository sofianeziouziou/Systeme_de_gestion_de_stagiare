// DESTINATION : src/main/java/com/hikma/stagiaires/service/ProjetService.java
package com.hikma.stagiaires.service.projet;

import com.hikma.stagiaires.dto.projet.ProjetDTOs.*;
import com.hikma.stagiaires.model.notification.NotificationType;
import com.hikma.stagiaires.model.projet.Projet;
import com.hikma.stagiaires.model.projet.Projet.TuteurAcceptation;
import com.hikma.stagiaires.model.projet.ProjetStatus;
import com.hikma.stagiaires.model.user.AccountStatus;
import com.hikma.stagiaires.model.user.Role;
import com.hikma.stagiaires.model.user.User;
import com.hikma.stagiaires.repository.ProjetRepository;
import com.hikma.stagiaires.repository.StagiaireRepository;
import com.hikma.stagiaires.repository.UserRepository;
import com.hikma.stagiaires.service.commun.AuditLogService;
import com.hikma.stagiaires.service.notification.EmailNotificationService;
import com.hikma.stagiaires.service.commun.FileStorageService;
import com.hikma.stagiaires.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjetService {

    private final ProjetRepository         projetRepository;
    private final StagiaireRepository      stagiaireRepository;
    private final UserRepository           userRepository;
    private final NotificationService      notificationService;
    private final FileStorageService       fileStorageService;
    private final AuditLogService          auditLogService;
    private final EmailNotificationService emailNotificationService;

    // ─────────────────────────────────────────────────────────────────────
    // CRÉER un projet (RH)
    // ─────────────────────────────────────────────────────────────────────
    public ProjetResponse create(CreateRequest req, String rhId) {

        if (req.getStagiaireIds() != null) {
            for (String sid : req.getStagiaireIds()) {
                boolean hasActive = projetRepository
                        .findByStagiaireIdsContainingAndDeletedFalse(sid)
                        .stream()
                        .anyMatch(p -> p.getStatus() != ProjetStatus.TERMINE
                                && p.getStatus() != ProjetStatus.ANNULE);
                if (hasActive) {
                    String nom = stagiaireRepository.findById(sid)
                            .map(s -> s.getFirstName() + " " + s.getLastName())
                            .orElseGet(() -> stagiaireRepository.findByUserId(sid)
                                    .map(s -> s.getFirstName() + " " + s.getLastName())
                                    .orElseGet(() -> userRepository.findById(sid)
                                            .map(u -> u.getFirstName() + " " + u.getLastName())
                                            .orElse(sid)));
                    throw new IllegalArgumentException(
                            "Le stagiaire " + nom + " a déjà un projet actif en cours.");
                }
            }
        }

        Projet projet = Projet.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .stagiaireIds(req.getStagiaireIds())
                .tuteurId(req.getTuteurId())
                .startDate(req.getStartDate())
                .plannedEndDate(req.getPlannedEndDate())
                .technologies(req.getTechnologies() != null ? req.getTechnologies() : List.of())
                .departement(req.getDepartement())
                .sprints(List.of())
                .tuteurAcceptation(TuteurAcceptation.PENDING)
                .build();

        Projet saved = projetRepository.save(projet);
        auditLogService.log(rhId, "CREATE", "PROJET", saved.getId(), null);

        if (req.getStagiaireIds() != null && req.getTuteurId() != null) {
            for (String sid : req.getStagiaireIds()) {
                stagiaireRepository.findById(sid).ifPresentOrElse(stagiaire -> {
                    if (stagiaire.getTuteurId() == null || stagiaire.getTuteurId().isBlank()) {
                        stagiaire.setTuteurId(req.getTuteurId());
                        stagiaireRepository.save(stagiaire);
                        log.info("[PROJET] tuteurId mis à jour pour stagiaire _id={}", sid);
                    }
                }, () -> stagiaireRepository.findByUserId(sid).ifPresent(stagiaire -> {
                    if (stagiaire.getTuteurId() == null || stagiaire.getTuteurId().isBlank()) {
                        stagiaire.setTuteurId(req.getTuteurId());
                        stagiaireRepository.save(stagiaire);
                        log.info("[PROJET] tuteurId mis à jour pour stagiaire userId={}", sid);
                    }
                }));
            }
        }

        if (req.getTuteurId() != null) {
            notificationService.createNotification(
                    req.getTuteurId(),
                    NotificationType.PROJET_ASSIGNE,
                    "Nouveau projet à accepter : " + req.getTitle(),
                    "Le RH vous a assigné un projet. Connectez-vous pour l'accepter ou le refuser.",
                    saved.getId());
            emailNotificationService.envoyerEmailNouveauProjetAvecAcceptation(saved);
        }

        log.info("[PROJET] Créé id={} statut acceptation=PENDING", saved.getId());
        return toResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────
    // TUTEUR ACCEPTE le projet
    // ─────────────────────────────────────────────────────────────────────
    public ProjetResponse accepter(String projetId, String tuteurUserId, String message) {
        Projet p = findActiveById(projetId);

        if (!tuteurUserId.equals(p.getTuteurId()))
            throw new IllegalArgumentException("Ce projet ne vous est pas assigné.");
        if (TuteurAcceptation.ACCEPTED.equals(p.getTuteurAcceptation()))
            throw new IllegalArgumentException("Ce projet est déjà accepté.");

        p.setTuteurAcceptation(TuteurAcceptation.ACCEPTED);
        Projet saved = projetRepository.save(p);
        log.info("[PROJET] Accepté id={} par tuteurId={}", projetId, tuteurUserId);

        emailNotificationService.envoyerEmailProjetAccepte(saved, message);

        if (p.getStagiaireIds() != null) {
            String tuteurNom = userRepository.findById(tuteurUserId)
                    .map(u -> u.getFirstName() + " " + u.getLastName())
                    .orElse("Le tuteur");
            for (String sid : p.getStagiaireIds()) {
                notificationService.createNotification(
                        sid, NotificationType.PROJET_ASSIGNE,
                        "Projet accepté : " + p.getTitle(),
                        tuteurNom + " a accepté d'encadrer votre projet. Il commence maintenant !",
                        projetId);
            }
        }

        userRepository.findByRoleAndAccountStatus(Role.RH, AccountStatus.APPROUVE)
                .forEach(rh -> notificationService.createNotification(
                        rh.getId(), NotificationType.PROJET_ASSIGNE,
                        "Projet accepté : " + p.getTitle(),
                        "Le tuteur a accepté d'encadrer le projet \"" + p.getTitle() + "\".",
                        projetId));

        return toResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────
    // TUTEUR REFUSE le projet
    // ─────────────────────────────────────────────────────────────────────
    public ProjetResponse refuser(String projetId, String tuteurUserId, String raison) {
        Projet p = findActiveById(projetId);

        if (!tuteurUserId.equals(p.getTuteurId()))
            throw new IllegalArgumentException("Ce projet ne vous est pas assigné.");
        if (TuteurAcceptation.ACCEPTED.equals(p.getTuteurAcceptation()))
            throw new IllegalArgumentException("Vous avez déjà accepté ce projet.");

        p.setTuteurAcceptation(TuteurAcceptation.REFUSED);
        p.setTuteurRefusRaison(raison != null ? raison : "Aucune raison fournie");
        Projet saved = projetRepository.save(p);
        log.info("[PROJET] Refusé id={} par tuteurId={} raison={}", projetId, tuteurUserId, raison);

        emailNotificationService.envoyerEmailProjetRefuse(saved, raison);

        String tuteurNom = userRepository.findById(tuteurUserId)
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse("Le tuteur");

        userRepository.findByRoleAndAccountStatus(Role.RH, AccountStatus.APPROUVE)
                .forEach(rh -> notificationService.createNotification(
                        rh.getId(), NotificationType.PROJET_EN_RETARD,
                        "⚠️ Projet refusé : " + p.getTitle(),
                        tuteurNom + " a refusé d'encadrer ce projet."
                                + (raison != null ? " Raison : " + raison : "")
                                + " Veuillez assigner un autre tuteur.",
                        projetId));

        return toResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────
    // RH RÉASSIGNE un nouveau tuteur
    // ─────────────────────────────────────────────────────────────────────
    public ProjetResponse reassignerTuteur(String projetId, String nouveauTuteurId, String rhId) {
        Projet p = findActiveById(projetId);

        if (!TuteurAcceptation.REFUSED.equals(p.getTuteurAcceptation()))
            throw new IllegalArgumentException(
                    "Le projet n'est pas en état REFUSED. Statut actuel : " + p.getTuteurAcceptation());

        User nouveauTuteur = userRepository.findById(nouveauTuteurId)
                .orElseThrow(() -> new NoSuchElementException("Tuteur introuvable : " + nouveauTuteurId));
        if (!Role.TUTEUR.equals(nouveauTuteur.getRole()))
            throw new IllegalArgumentException("Cet utilisateur n'est pas un tuteur.");

        String ancienTuteurId = p.getTuteurId();
        p.setTuteurId(nouveauTuteurId);
        p.setTuteurAcceptation(TuteurAcceptation.PENDING);
        p.setTuteurRefusRaison(null);
        Projet saved = projetRepository.save(p);

        auditLogService.log(rhId, "REASSIGN_TUTEUR", "PROJET", projetId, null);
        log.info("[PROJET] Tuteur réassigné id={} ancien={} nouveau={}", projetId, ancienTuteurId, nouveauTuteurId);

        if (p.getStagiaireIds() != null) {
            for (String sid : p.getStagiaireIds()) {
                stagiaireRepository.findById(sid).ifPresentOrElse(s -> {
                    s.setTuteurId(nouveauTuteurId);
                    stagiaireRepository.save(s);
                }, () -> stagiaireRepository.findByUserId(sid).ifPresent(s -> {
                    s.setTuteurId(nouveauTuteurId);
                    stagiaireRepository.save(s);
                }));
            }
        }

        notificationService.createNotification(
                nouveauTuteurId, NotificationType.PROJET_ASSIGNE,
                "Nouveau projet à accepter : " + p.getTitle(),
                "Le RH vous a assigné un projet suite à un refus. Connectez-vous pour l'accepter.",
                projetId);
        emailNotificationService.envoyerEmailNouveauProjetAvecAcceptation(saved);

        return toResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────
    // GETTERS
    // ─────────────────────────────────────────────────────────────────────

    public ProjetResponse getById(String id) {
        return toResponse(findActiveById(id));
    }

    public Page<ProjetResponse> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return projetRepository.findByDeletedFalse(pageable).map(this::toResponse);
    }

    public List<ProjetResponse> getByTuteur(String tuteurId) {
        return projetRepository.findByTuteurIdAndDeletedFalse(tuteurId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ✅ FIX — double lookup : userId direct + stagiaire._id
    public List<ProjetResponse> getByStagiaire(String userId) {
        // 1. Cherche par userId direct (cas futur où stagiaireIds stocke userId)
        List<Projet> projets = new ArrayList<>(
                projetRepository.findByStagiaireIdsContainingAndDeletedFalse(userId));

        // 2. ✅ Cherche par stagiaire._id (cas actuel dans MongoDB)
        if (projets.isEmpty()) {
            String stagiaireId = stagiaireRepository.findByUserId(userId)
                    .map(s -> s.getId())
                    .orElse(null);
            if (stagiaireId != null && !stagiaireId.equals(userId)) {
                List<Projet> parId = projetRepository
                        .findByStagiaireIdsContainingAndDeletedFalse(stagiaireId);
                projets.addAll(parId);
                log.info("[PROJET] getByStagiaire userId={} → stagiaireId={} → {} projets",
                        userId, stagiaireId, parId.size());
            }
        }

        return projets.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<ProjetResponse> getPendingForTuteur(String tuteurId) {
        return projetRepository.findByTuteurIdAndDeletedFalse(tuteurId)
                .stream()
                .filter(p -> TuteurAcceptation.PENDING.equals(p.getTuteurAcceptation()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ProjetResponse> getRefusedProjets() {
        return projetRepository.findByDeletedFalse(Pageable.unpaged())
                .stream()
                .filter(p -> TuteurAcceptation.REFUSED.equals(p.getTuteurAcceptation()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────────────

    public ProjetResponse update(String id, UpdateRequest req, String userId) {
        Projet p = findActiveById(id);

        if (req.getTitle()          != null) p.setTitle(req.getTitle());
        if (req.getDescription()    != null) p.setDescription(req.getDescription());
        if (req.getStagiaireIds()   != null) p.setStagiaireIds(req.getStagiaireIds());
        if (req.getTuteurId()       != null) p.setTuteurId(req.getTuteurId());
        if (req.getStartDate()      != null) p.setStartDate(req.getStartDate());
        if (req.getPlannedEndDate() != null) p.setPlannedEndDate(req.getPlannedEndDate());
        if (req.getActualEndDate()  != null) p.setActualEndDate(req.getActualEndDate());
        if (req.getTechnologies()   != null) p.setTechnologies(req.getTechnologies());
        if (req.getStatus()         != null) p.setStatus(req.getStatus());
        if (req.getDepartement()    != null) p.setDepartement(req.getDepartement());

        if (req.getProgress() != null) {
            p.setProgress(req.getProgress());
            if (req.getProgress() >= 100) {
                p.setStatus(ProjetStatus.TERMINE);
                p.setActualEndDate(LocalDate.now());
                emailNotificationService.envoyerEmailProjetTermine(p);
            }
        }

        if (req.getSprints() != null)
            p.setSprints(buildSprints(req.getSprints()));

        return toResponse(projetRepository.save(p));
    }

    // ─────────────────────────────────────────────────────────────────────
    // SPRINT
    // ─────────────────────────────────────────────────────────────────────

    public ProjetResponse completeSprint(String projetId, String sprintId, String userId) {
        Projet p = findActiveById(projetId);

        List<Projet.Sprint> updated = p.getSprints().stream().map(s -> {
            if (sprintId.equals(s.getId())) s.setStatus("TERMINE");
            return s;
        }).collect(Collectors.toList());

        p.setSprints(updated);

        long total    = updated.size();
        long termines = updated.stream().filter(s -> "TERMINE".equals(s.getStatus())).count();

        if (total > 0) {
            int progress = (int) Math.round((double) termines / total * 100);
            p.setProgress(progress);
            if (progress >= 100) {
                p.setStatus(ProjetStatus.TERMINE);
                p.setActualEndDate(LocalDate.now());
                emailNotificationService.envoyerEmailProjetTermine(p);
            }
        }

        return toResponse(projetRepository.save(p));
    }

    public ProjetResponse uploadReport(String id, MultipartFile file, String userId) {
        Projet p = findActiveById(id);
        String url = fileStorageService.uploadFile(file, "rapports/" + id);
        p.setReportUrl(url);
        p.setReportSubmittedAt(LocalDate.now());
        return toResponse(projetRepository.save(p));
    }

    public void softDelete(String id, String userId) {
        Projet p = findActiveById(id);
        p.setDeleted(true);
        projetRepository.save(p);
        auditLogService.log(userId, "DELETE", "PROJET", id, null);
    }

    // ─────────────────────────────────────────────────────────────────────
    // SCHEDULER
    // ─────────────────────────────────────────────────────────────────────

    @Scheduled(cron = "0 0 8 * * *")
    public void checkProjectDeadlines() {
        LocalDate today   = LocalDate.now();
        LocalDate in3Days = today.plusDays(3);
        LocalDate in7Days = today.plusDays(7);

        projetRepository
                .findByDeletedFalseAndStatusAndPlannedEndDateBetween(ProjetStatus.EN_COURS, today, in3Days)
                .stream()
                .filter(p -> TuteurAcceptation.ACCEPTED.equals(p.getTuteurAcceptation()))
                .forEach(p -> {
                    notificationService.notifyDeadlineProche(p);
                    emailNotificationService.envoyerEmailDeadlineProche(p);
                });

        projetRepository
                .findByDeletedFalseAndStatusAndPlannedEndDateBetween(ProjetStatus.EN_COURS, in3Days, in7Days)
                .stream()
                .filter(p -> TuteurAcceptation.ACCEPTED.equals(p.getTuteurAcceptation()))
                .forEach(p -> notificationService.notifyDeadlineProche(p));

        List<ProjetStatus> exclus = List.of(ProjetStatus.TERMINE, ProjetStatus.ANNULE, ProjetStatus.SUSPENDU);

        projetRepository
                .findByDeletedFalseAndStatusNotInAndPlannedEndDateBefore(exclus, today)
                .stream()
                .filter(p -> TuteurAcceptation.ACCEPTED.equals(p.getTuteurAcceptation()))
                .forEach(p -> {
                    if (!ProjetStatus.EN_RETARD.equals(p.getStatus())) {
                        p.setStatus(ProjetStatus.EN_RETARD);
                        projetRepository.save(p);
                        emailNotificationService.envoyerEmailProjetEnRetard(p);
                    }
                    notificationService.notifyProjetEnRetard(p);
                });

        LocalDateTime fiveDaysAgo = LocalDateTime.now().minusDays(5);
        projetRepository
                .findByDeletedFalseAndStatusAndUpdatedAtBefore(ProjetStatus.EN_COURS, fiveDaysAgo)
                .stream()
                .filter(p -> TuteurAcceptation.ACCEPTED.equals(p.getTuteurAcceptation()))
                .forEach(p -> notificationService.notifySansMiseAJour(p));

        projetRepository.findByDeletedFalse(Pageable.unpaged()).forEach(p -> {
            if (p.getSprints() == null || p.getSprints().isEmpty()) return;
            boolean changed = false;
            for (Projet.Sprint sprint : p.getSprints()) {
                if ("TERMINE".equals(sprint.getStatus())) continue;
                if (sprint.getEndDate() != null && sprint.getEndDate().isBefore(today)) {
                    sprint.setStatus("EN_RETARD");
                    changed = true;
                    notificationService.createNotification(
                            p.getTuteurId(), NotificationType.PROJET_EN_RETARD,
                            "Sprint en retard",
                            "Le sprint \"" + sprint.getTitle() + "\" du projet \""
                                    + p.getTitle() + "\" a dépassé sa date de fin.",
                            p.getId());
                }
            }
            if (changed) projetRepository.save(p);
        });

        projetRepository.findByDeletedFalse(Pageable.unpaged())
                .stream()
                .filter(p -> TuteurAcceptation.PENDING.equals(p.getTuteurAcceptation()))
                .filter(p -> p.getCreatedAt() != null
                        && p.getCreatedAt().isBefore(LocalDateTime.now().minusHours(24)))
                .forEach(p -> {
                    log.warn("[SCHEDULER] Projet {} en attente d'acceptation depuis +24h", p.getId());
                    emailNotificationService.envoyerEmailRappelAcceptation(p);
                });

        log.info("[SCHEDULER] checkProjectDeadlines terminé — {}", today);
    }

    // ─────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────

    private List<Projet.Sprint> buildSprints(List<SprintRequest> requests) {
        if (requests == null) return new ArrayList<>();
        return requests.stream().map(r -> {
            Projet.Sprint s = new Projet.Sprint();
            s.setId(r.getId() != null ? r.getId() : UUID.randomUUID().toString());
            s.setTitle(r.getTitle());
            s.setDescription(r.getDescription());
            s.setStartDate(r.getStartDate());
            s.setEndDate(r.getEndDate());
            s.setStagiaireId(r.getStagiaireId());
            s.setStatus(r.getStatus() != null ? r.getStatus() : "EN_COURS");
            return s;
        }).collect(Collectors.toList());
    }

    private Projet findActiveById(String id) {
        return projetRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new NoSuchElementException("Projet introuvable : " + id));
    }

    // ─────────────────────────────────────────────────────────────────────
    // toResponse — triple lookup stagiaires
    // ─────────────────────────────────────────────────────────────────────
    public ProjetResponse toResponse(Projet p) {
        ProjetResponse r = new ProjetResponse();
        r.setId(p.getId());
        r.setTitle(p.getTitle());
        r.setDescription(p.getDescription());
        r.setStagiaireIds(p.getStagiaireIds());
        r.setTuteurId(p.getTuteurId());
        r.setStartDate(p.getStartDate());
        r.setPlannedEndDate(p.getPlannedEndDate());
        r.setActualEndDate(p.getActualEndDate());
        r.setProgress(p.getProgress());
        r.setStatus(p.getStatus());
        r.setTechnologies(p.getTechnologies());
        r.setReportUrl(p.getReportUrl());
        r.setReportSubmittedAt(p.getReportSubmittedAt());
        r.setCreatedAt(p.getCreatedAt());
        r.setUpdatedAt(p.getUpdatedAt());
        r.setDepartement(p.getDepartement());
        r.setTuteurAcceptation(p.getTuteurAcceptation());
        r.setTuteurRefusRaison(p.getTuteurRefusRaison());

        // Sprints
        LocalDate today = LocalDate.now();
        if (p.getSprints() != null) {
            r.setSprints(p.getSprints().stream().map(s -> {
                SprintResponse sr = new SprintResponse();
                sr.setId(s.getId());
                sr.setTitle(s.getTitle());
                sr.setDescription(s.getDescription());
                sr.setStartDate(s.getStartDate());
                sr.setEndDate(s.getEndDate());
                sr.setStatus(s.getStatus());
                sr.setStagiaireId(s.getStagiaireId());
                sr.setOverdue(!("TERMINE".equals(s.getStatus()))
                        && s.getEndDate() != null
                        && s.getEndDate().isBefore(today));
                if (s.getStagiaireId() != null) {
                    stagiaireRepository.findById(s.getStagiaireId())
                            .ifPresentOrElse(
                                    st -> sr.setStagiaireName(st.getFirstName() + " " + st.getLastName()),
                                    () -> stagiaireRepository.findByUserId(s.getStagiaireId())
                                            .ifPresentOrElse(
                                                    st -> sr.setStagiaireName(st.getFirstName() + " " + st.getLastName()),
                                                    () -> userRepository.findById(s.getStagiaireId())
                                                            .ifPresent(u -> sr.setStagiaireName(u.getFirstName() + " " + u.getLastName()))
                                            )
                            );
                }
                return sr;
            }).collect(Collectors.toList()));
        }

        // Nom tuteur
        if (p.getTuteurId() != null) {
            userRepository.findById(p.getTuteurId())
                    .ifPresent(u -> r.setTuteurName(u.getFirstName() + " " + u.getLastName()));
        }

        // ✅ Stagiaires — triple lookup : stagiaires._id → stagiaires.userId → users._id
        if (p.getStagiaireIds() != null) {
            r.setStagiaires(p.getStagiaireIds().stream()
                    .filter(sid -> sid != null && !sid.isBlank())
                    .map(sid -> {
                        log.debug("[PROJET] lookup stagiaire sid={}", sid);
                        return stagiaireRepository.findById(sid)
                                .map(s -> {
                                    log.debug("[PROJET] trouvé par _id: {} {}", s.getFirstName(), s.getLastName());
                                    ProjetResponse.StagiaireInfo si = new ProjetResponse.StagiaireInfo();
                                    si.setId(s.getId());
                                    si.setFirstName(s.getFirstName());
                                    si.setLastName(s.getLastName());
                                    si.setPhotoUrl(s.getPhotoUrl());
                                    return si;
                                })
                                .orElseGet(() -> stagiaireRepository.findByUserId(sid)
                                        .map(s -> {
                                            log.debug("[PROJET] trouvé par userId: {} {}", s.getFirstName(), s.getLastName());
                                            ProjetResponse.StagiaireInfo si = new ProjetResponse.StagiaireInfo();
                                            si.setId(s.getId());
                                            si.setFirstName(s.getFirstName());
                                            si.setLastName(s.getLastName());
                                            si.setPhotoUrl(s.getPhotoUrl());
                                            return si;
                                        })
                                        .orElseGet(() -> userRepository.findById(sid)
                                                .map(u -> {
                                                    log.debug("[PROJET] trouvé dans users: {} {}", u.getFirstName(), u.getLastName());
                                                    ProjetResponse.StagiaireInfo si = new ProjetResponse.StagiaireInfo();
                                                    si.setId(sid);
                                                    si.setFirstName(u.getFirstName());
                                                    si.setLastName(u.getLastName());
                                                    return si;
                                                })
                                                .orElseGet(() -> {
                                                    log.warn("[PROJET] stagiaire introuvable pour sid={}", sid);
                                                    return null;
                                                })
                                        )
                                );
                    })
                    .filter(si -> si != null)
                    .collect(Collectors.toList()));
        }

        return r;
    }
}