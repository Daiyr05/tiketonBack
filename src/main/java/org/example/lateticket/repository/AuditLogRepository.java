package org.example.lateticket.repository;

import org.example.lateticket.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByEventIdOrderByTimestampDesc(Long eventId);
    List<AuditLog> findByUserIdOrderByTimestampDesc(Long userId);
    List<AuditLog> findByActionTypeInOrderByTimestampDesc(List<String> actionTypes);
}