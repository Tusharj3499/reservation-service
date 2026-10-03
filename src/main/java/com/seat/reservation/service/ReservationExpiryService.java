package com.seat.reservation.service;

import com.seat.reservation.entity.Reservation;
import com.seat.reservation.entity.ReservationSeat;
import com.seat.reservation.entity.ReservationStatus;
import com.seat.reservation.entity.Seat;
import com.seat.reservation.entity.SeatStatus;
import com.seat.reservation.repository.ReservationRepository;
import com.seat.reservation.repository.ReservationSeatRepository;
import com.seat.reservation.repository.SeatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservationExpiryService {

    private static final Logger logger =
            LoggerFactory.getLogger(ReservationExpiryService.class);

    private final ReservationRepository reservationRepository;
    private final ReservationSeatRepository reservationSeatRepository;
    private final SeatRepository seatRepository;

    public ReservationExpiryService(
            ReservationRepository reservationRepository,
            ReservationSeatRepository reservationSeatRepository,
            SeatRepository seatRepository) {

        this.reservationRepository = reservationRepository;
        this.reservationSeatRepository = reservationSeatRepository;
        this.seatRepository = seatRepository;
    }

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void expireReservations() {

        List<Reservation> expiredReservations =
                reservationRepository
                        .findExpiredHeldReservations(
                                ReservationStatus.HELD,
                                LocalDateTime.now());

        for (Reservation reservation : expiredReservations) {

            List<ReservationSeat> reservationSeats =
                    reservationSeatRepository
                            .findByReservationId(
                                    reservation.getId());

            List<Long> seatIds = reservationSeats.stream()
                    .map(reservationSeat ->
                            reservationSeat.getSeat().getId())
                    .sorted()
                    .toList();

            seatRepository.findAllByIdForUpdate(seatIds);

            for (ReservationSeat reservationSeat : reservationSeats) {

                Seat seat = reservationSeat.getSeat();

                if (seat.getStatus() == SeatStatus.HELD) {

                    seat.setStatus(SeatStatus.AVAILABLE);

                    seatRepository.save(seat);
                }
            }

            reservation.setStatus(ReservationStatus.EXPIRED);
            reservation.setHoldExpiresAt(null);

            reservationRepository.save(reservation);

            logger.info(
                    "Reservation expired: reservationId={}, userId={}",
                    reservation.getId(),
                    reservation.getUserId());
        }
    }
}