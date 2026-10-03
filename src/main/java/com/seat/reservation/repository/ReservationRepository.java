package com.seat.reservation.repository;

import com.seat.reservation.entity.Reservation;
import com.seat.reservation.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    long countByUserIdAndStatus(Long userId, ReservationStatus status);
}
