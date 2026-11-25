package org.example.lateticket.repository;

import org.example.lateticket.entity.EventReport;
import org.example.lateticket.entity.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventReportRepository extends JpaRepository<EventReport, Long> {
    List<EventReport> findAllByOrderByCreatedAtDesc();
    List<EventReport> findByStatusOrderByCreatedAtDesc(ReportStatus status);
    boolean existsByEventIdAndReporterIdAndStatus(Long eventId, Long reporterId, ReportStatus status);
}
