package org.example.lateticket.service;

import lombok.RequiredArgsConstructor;
import org.example.lateticket.entity.AuditLog;
import org.example.lateticket.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void log(Long eventId, Long userId, String actionType,
                    String entityType, Long entityId, String description) {
        AuditLog log = AuditLog.builder()
                .eventId(eventId)
                .userId(userId)
                .actionType(actionType)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .build();
        auditLogRepository.save(log);
    }

    public List<AuditLog> getEventTimeline(Long eventId) {
        return auditLogRepository.findByEventIdOrderByTimestampDesc(eventId);
    }
}