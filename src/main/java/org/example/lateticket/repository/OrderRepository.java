package org.example.lateticket.repository;

import org.example.lateticket.entity.Order;
import org.example.lateticket.entity.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByAttendeeId(Long attendeeId);
    List<Order> findByEventId(Long eventId);
    List<Order> findByEventIdAndStatus(Long eventId, OrderStatus status);
    long countByEventIdAndStatus(Long eventId, OrderStatus status);
}