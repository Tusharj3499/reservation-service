package com.seat.reservation.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public class CreateReservationRequest {

    @NotNull
    @Positive
    private Long userId;

    @NotEmpty
    private List<@NotNull @Positive Long> seatIds;

    @NotNull
    @Positive
    private Long totalAmountPaise;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<Long> getSeatIds() {
        return seatIds;
    }

    public void setSeatIds(List<Long> seatIds) {
        this.seatIds = seatIds;
    }

    public Long getTotalAmountPaise() {
        return totalAmountPaise;
    }

    public void setTotalAmountPaise(Long totalAmountPaise) {
        this.totalAmountPaise = totalAmountPaise;
    }
}
