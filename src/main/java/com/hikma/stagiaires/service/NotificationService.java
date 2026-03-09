package com.hikma.stagiaires.service;

import com.hikma.stagiaires.model.*;
import com.hikma.stagiaires.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public void notifyDeadlineProche(Projet projet) {
        createNotification(projet.getTuteurId(),
                NotificationType.PROJET_DEADLINE_PROCHE,
                "Deadline approche",
                "Le projet \"" + projet.getTitle() + "\" arrive à échéance dans moins de 7 jours.",
                projet.getId());
    }

    public void notifyProjetEnRetard(Projet projet) {
        createNotification(projet.getTuteurId(),
                NotificationType.PROJET_EN_RETARD,
                "Projet en retard",
                "Le projet \"" + projet.getTitle() + "\" a dépassé sa date de fin prévue.",
                projet.getId());
    }

    public void notifySansMiseAJour(Projet projet) {
        createNotification(projet.getTuteurId(),
                NotificationType.PROJET_SANS_MISE_A_JOUR,
                "Projet sans mise à jour",
                "Le projet \"" + projet.getTitle() + "\" n'a pas été mis à jour depuis 5 jours.",
                projet.getId());
    }

    public void notifyEvaluationSoumise(Evaluation evaluation) {
        // Notifier RH (à personnaliser selon la config)
        log.info("Évaluation soumise pour stagiaire {}", evaluation.getStagiaireId());
    }

    public void notifyEvaluationValidee(Evaluation evaluation) {
        createNotification(evaluation.getStagiaireId(),
                NotificationType.EVALUATION_VALIDEE,
                "Évaluation validée",
                "Votre évaluation a été validée. Score : " + evaluation.getScoreGlobal(),
                evaluation.getId());
    }

    public List<Notification> getForUser(String userId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId);
    }

    public long countUnread(String userId) {
        return notificationRepository.countByRecipientIdAndReadFalse(userId);
    }

    public void markAsRead(String notificationId, String userId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getRecipientId().equals(userId)) {
                n.setRead(true);
                n.setReadAt(java.time.LocalDateTime.now());
                notificationRepository.save(n);
            }
        });
    }

    private void createNotification(String recipientId, NotificationType type,
                                    String title, String message, String relatedEntityId) {
        Notification notif = Notification.builder()
                .recipientId(recipientId)
                .type(type)
                .title(title)
                .message(message)
                .relatedEntityId(relatedEntityId)
                .build();
        notificationRepository.save(notif);
    }
}