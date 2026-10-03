package com.seat.reservation.exception;

import org.hibernate.JDBCException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.CannotCreateTransactionException;
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

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> handleDatabaseException(
            DataAccessException exception) {

        return serviceUnavailableResponse();
    }

    @ExceptionHandler(CannotCreateTransactionException.class)
    public ResponseEntity<Map<String, String>> handleTransactionException(
            CannotCreateTransactionException exception) {

        return serviceUnavailableResponse();
    }

    @ExceptionHandler(JDBCException.class)
    public ResponseEntity<Map<String, String>> handleJdbcException(
            JDBCException exception) {

        return serviceUnavailableResponse();
    }

    private ResponseEntity<Map<String, String>> serviceUnavailableResponse() {

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "SERVICE_UNAVAILABLE",
                        "message",
                        "Reservation service is temporarily unavailable"
                ));
    }
}