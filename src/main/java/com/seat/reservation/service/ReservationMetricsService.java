package com.seat.reservation.service;

import com.seat.reservation.entity.SeatStatus;
import com.seat.reservation.repository.SeatRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ReservationMetricsService {

    private final Counter reservationsConfirmed;
    private final Counter reservationsDeclinedSeatTaken;
    private final Counter reservationsDeclinedUserLimit;
    private final Counter reservationsDeclinedIdempotentReplay;

    private final AtomicInteger seatsAvailable;

    private final SeatRepository seatRepository;

    public ReservationMetricsService(
            MeterRegistry meterRegistry,
            SeatRepository seatRepository) {

        this.seatRepository = seatRepository;

        reservationsConfirmed = Counter.builder(
                        "reservations_confirmed_total")
                .description(
                        "Total number of confirmed reservations")
                .register(meterRegistry);

        reservationsDeclinedSeatTaken = Counter.builder(
                        "reservations_declined_total")
                .tag("reason", "seat-taken")
                .description(
                        "Reservations declined by reason")
                .register(meterRegistry);

        reservationsDeclinedUserLimit = Counter.builder(
                        "reservations_declined_total")
                .tag("reason", "per-user-limit")
                .description(
                        "Reservations declined by reason")
                .register(meterRegistry);

        reservationsDeclinedIdempotentReplay = Counter.builder(
                        "reservations_declined_total")
                .tag("reason", "idempotent-replay")
                .description(
                        "Reservations declined by reason")
                .register(meterRegistry);

        seatsAvailable = new AtomicInteger();

        Gauge.builder(
                        "seats_available",
                        seatsAvailable,
                        AtomicInteger::get)
                .description(
                        "Number of currently available seats")
                .register(meterRegistry);
    }

    public void reservationConfirmed() {
        reservationsConfirmed.increment();
    }

    public void reservationDeclinedSeatTaken() {
        reservationsDeclinedSeatTaken.increment();
    }

    public void reservationDeclinedUserLimit() {
        reservationsDeclinedUserLimit.increment();
    }

    public void reservationDeclinedIdempotentReplay() {
        reservationsDeclinedIdempotentReplay.increment();
    }

    public void updateSeatsAvailable() {

        int availableSeats =
                (int) seatRepository.countByStatus(
                        SeatStatus.AVAILABLE);

        seatsAvailable.set(availableSeats);
    }

    @Scheduled(fixedDelay = 5000)
    public void refreshAvailableSeats() {

        updateSeatsAvailable();
    }

    @PostConstruct
    public void initializeAvailableSeats() {
        updateSeatsAvailable();
    }
}