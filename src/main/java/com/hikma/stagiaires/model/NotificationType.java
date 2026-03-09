package com.hikma.stagiaires.model;

public enum NotificationType {
    PROJET_DEADLINE_PROCHE,      // Projet à moins de 7 jours de sa deadline
    PROJET_EN_RETARD,            // Projet dépasse sa date de fin
    PROJET_SANS_MISE_A_JOUR,     // Avancement non mis à jour depuis 5 jours
    EVALUATION_SOUMISE,
    EVALUATION_VALIDEE,
    NOUVEAU_STAGIAIRE,
    RAPPEL_RAPPORT
}