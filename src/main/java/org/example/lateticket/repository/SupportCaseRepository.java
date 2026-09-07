package org.example.lateticket.repository;

import org.example.lateticket.entity.SupportCase;
import org.example.lateticket.entity.enums.SupportCaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportCaseRepository extends JpaRepository<SupportCase, Long> {
    List<SupportCase> findByUserId(Long userId);
    List<SupportCase> findByEventId(Long eventId);
    List<SupportCase> findByStatus(SupportCaseStatus status);
    List<SupportCase> findByAssignedToId(Long assignedToId);
}