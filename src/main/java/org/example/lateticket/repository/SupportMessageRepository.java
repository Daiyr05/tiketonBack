package org.example.lateticket.repository;

import org.example.lateticket.entity.SupportMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long> {
    List<SupportMessage> findBySupportCaseIdOrderByCreatedAtAsc(Long caseId);
}