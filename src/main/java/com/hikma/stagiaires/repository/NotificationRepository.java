package com.hikma.stagiaires.repository;

import com.hikma.stagiaires.model.notification.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(String recipientId);
    List<Notification> findByRecipientIdAndReadFalse(String recipientId);
    long countByRecipientIdAndReadFalse(String recipientId);
}