package com.hikma.stagiaires.service.commun;

import com.hikma.stagiaires.model.commun.AuditLog;
import com.hikma.stagiaires.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Async
    public void log(String userId, String action, String resource, String resourceId, String ipAddress) {
        AuditLog log = AuditLog.builder()
                .userId(userId)
                .action(action)
                .resource(resource)
                .resourceId(resourceId)
                .ipAddress(ipAddress)
                .build();
        auditLogRepository.save(log);
    }
}