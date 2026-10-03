package com.seat.reservation.exception;

public class SeatUnavailableException extends ReservationException {

    public SeatUnavailableException(String message) {
        super(message);
    }
}