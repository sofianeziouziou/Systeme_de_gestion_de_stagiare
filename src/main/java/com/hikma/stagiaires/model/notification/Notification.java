package com.hikma.stagiaires.model.notification;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notifications")
public class Notification {

    @Id
    private String id;

    private String recipientId;       // userId du destinataire
    private NotificationType type;

    private String title;
    private String message;
    private String relatedEntityId;   // projetId ou stagiaireId lié

    @Builder.Default
    private boolean read = false;

    private LocalDateTime readAt;

    @CreatedDate
    private LocalDateTime createdAt;
}