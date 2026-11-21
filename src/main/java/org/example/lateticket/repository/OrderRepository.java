package org.example.lateticket.repository;

import org.example.lateticket.entity.Order;
import org.example.lateticket.entity.enums.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByAttendeeId(Long attendeeId);
    List<Order> findByEventId(Long eventId);
    List<Order> findByEventIdAndStatus(Long eventId, OrderStatus status);
    long countByEventIdAndStatus(Long eventId, OrderStatus status);

    @Query("select o.id from Order o where o.status = :status and o.createdAt < :before")
    List<Long> findIdsByStatusAndCreatedAtBefore(@Param("status") OrderStatus status,
                                                 @Param("before") LocalDateTime before);

    // Serializes pay / cancel / expiry on the same order
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> lockById(@Param("id") Long id);
}