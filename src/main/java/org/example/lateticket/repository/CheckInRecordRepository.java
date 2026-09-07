package org.example.lateticket.repository;

import org.example.lateticket.entity.CheckInRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CheckInRecordRepository extends JpaRepository<CheckInRecord, Long> {
    List<CheckInRecord> findByTicketId(Long ticketId);
    List<CheckInRecord> findByEventId(Long eventId);
    long countByEventIdAndReversedFalse(Long eventId);
}