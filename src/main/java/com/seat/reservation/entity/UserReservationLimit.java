package com.seat.reservation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_reservation_limits")
public class UserReservationLimit {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "confirmed_count", nullable = false)
    private int confirmedCount;

    public UserReservationLimit() {
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public int getConfirmedCount() {
        return confirmedCount;
    }

    public void setConfirmedCount(int confirmedCount) {
        this.confirmedCount = confirmedCount;
    }
}
