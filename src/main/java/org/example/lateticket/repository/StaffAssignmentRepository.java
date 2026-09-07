package org.example.lateticket.repository;

import org.example.lateticket.entity.StaffAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StaffAssignmentRepository extends JpaRepository<StaffAssignment, Long> {
    List<StaffAssignment> findByEventId(Long eventId);
    List<StaffAssignment> findByUserId(Long userId);
    Optional<StaffAssignment> findByEventIdAndUserId(Long eventId, Long userId);
    boolean existsByEventIdAndUserId(Long eventId, Long userId);
}