package com.hikma.stagiaires.model;

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
@Document(collection = "auditLogs")
public class AuditLog {

    @Id
    private String id;

    private String userId;
    private String userEmail;
    private String action;        // CREATE, READ, UPDATE, DELETE, LOGIN, LOGOUT...
    private String resource;      // STAGIAIRE, PROJET, EVALUATION, USER...
    private String resourceId;

    private String ipAddress;
    private String details;       // JSON ou description de l'action

    @CreatedDate
    private LocalDateTime timestamp;
}