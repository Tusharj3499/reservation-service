package com.seat.reservation.dto;

import com.seat.reservation.entity.ReservationStatus;
import java.util.List;

public class CreateReservationResponse {

    private Long reservationId;
    private Long userId;
    private List<Long> seatIds;
    private ReservationStatus status;
    private Long totalAmountPaise;

    public CreateReservationResponse(
            Long reservationId,
            Long userId,
            List<Long> seatIds,
            ReservationStatus status,
            Long totalAmountPaise) {

        this.reservationId = reservationId;
        this.userId = userId;
        this.seatIds = seatIds;
        this.status = status;
        this.totalAmountPaise = totalAmountPaise;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public Long getUserId() {
        return userId;
    }

    public List<Long> getSeatIds() {
        return seatIds;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public Long getTotalAmountPaise() {
        return totalAmountPaise;
    }
}