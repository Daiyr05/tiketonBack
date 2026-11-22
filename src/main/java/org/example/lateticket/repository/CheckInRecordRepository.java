package org.example.lateticket.repository;

import org.example.lateticket.entity.CheckInRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CheckInRecordRepository extends JpaRepository<CheckInRecord, Long> {
    List<CheckInRecord> findByTicketId(Long ticketId);
    List<CheckInRecord> findByEventId(Long eventId);
    long countByEventIdAndReversedFalse(Long eventId);
    long countByEventIdAndReversedTrue(Long eventId);
    Optional<CheckInRecord> findFirstByTicketIdAndReversedFalseOrderByTimestampAsc(Long ticketId);
    List<CheckInRecord> findTop20ByEventIdAndReversedFalseOrderByTimestampDesc(Long eventId);
}
