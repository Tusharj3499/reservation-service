package com.seat.reservation.controller;

import com.seat.reservation.dto.CreateReservationRequest;
import com.seat.reservation.dto.CreateReservationResponse;
import com.seat.reservation.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<CreateReservationResponse> createReservation(
            @Valid @RequestBody CreateReservationRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {

        CreateReservationResponse response =
                reservationService.createReservation(
                        request,
                        idempotencyKey);

        return ResponseEntity
                .status(201)
                .body(response);
    }
}
