package com.seat.reservation.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SeatUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleSeatUnavailable(
            SeatUnavailableException exception) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", "SEAT_UNAVAILABLE",
                        "message", exception.getMessage()
                ));
    }

    @ExceptionHandler(ReservationException.class)
    public ResponseEntity<Map<String, String>> handleReservationException(
            ReservationException exception) {

        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        "error", "RESERVATION_ERROR",
                        "message", exception.getMessage()
                ));
    }
}
