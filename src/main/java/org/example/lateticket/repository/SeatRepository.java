package org.example.lateticket.repository;

import jakarta.persistence.LockModeType;
import org.example.lateticket.entity.Seat;
import org.example.lateticket.entity.enums.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    // Seats are inserted in the order the editor sends sections/rows/seats, so id order is the layout order
    List<Seat> findByEventIdOrderByIdAsc(Long eventId);

    List<Seat> findByOrderId(Long orderId);

    boolean existsByEventId(Long eventId);

    boolean existsByEventIdAndStatus(Long eventId, SeatStatus status);

    // Row-level lock (SELECT ... FOR UPDATE) so two attendees can't grab the same seat concurrently.
    // Ordered by id to avoid deadlocks between overlapping requests.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seat s where s.event.id = :eventId and s.id in :ids order by s.id")
    List<Seat> lockByEventIdAndIds(@Param("eventId") Long eventId, @Param("ids") Collection<Long> ids);

    @Query("select count(s) from Seat s where s.event.id = :eventId and s.heldBy.id = :userId " +
            "and s.status = org.example.lateticket.entity.enums.SeatStatus.HELD and s.heldUntil > :now")
    long countActiveHolds(@Param("eventId") Long eventId, @Param("userId") Long userId,
                          @Param("now") LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seat s where s.event.id = :eventId and s.heldBy.id = :userId " +
            "and s.status = org.example.lateticket.entity.enums.SeatStatus.HELD order by s.id")
    List<Seat> lockHeldBy(@Param("eventId") Long eventId, @Param("userId") Long userId);

    @Modifying
    @Query("delete from Seat s where s.event.id = :eventId")
    void deleteByEventId(@Param("eventId") Long eventId);
}
