package com.seat.reservation.repository;

import com.seat.reservation.entity.Reservation;
import com.seat.reservation.entity.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository
        extends JpaRepository<Reservation, Long> {

    long countByUserIdAndStatus(
            Long userId,
            ReservationStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r
            FROM Reservation r
            WHERE r.id = :reservationId
            """)
    Optional<Reservation> findByIdForUpdate(
            @Param("reservationId") Long reservationId);

    @Query("""
            SELECT r
            FROM Reservation r
            WHERE r.status = :status
              AND r.holdExpiresAt <= :now
            """)
    List<Reservation> findExpiredHeldReservations(
            @Param("status") ReservationStatus status,
            @Param("now") LocalDateTime now);
}