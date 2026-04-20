package com.hikma.stagiaires.model.user;

public enum AccountStatus {
    EN_ATTENTE,   // Inscription soumise, en attente d'approbation RH
    APPROUVE,     // Compte approuvé, accès autorisé
    REFUSE        // Compte refusé par RH
}