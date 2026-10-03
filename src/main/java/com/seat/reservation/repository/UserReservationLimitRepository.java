package com.seat.reservation.repository;

import com.seat.reservation.entity.UserReservationLimit;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserReservationLimitRepository
        extends JpaRepository<UserReservationLimit, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT u
            FROM UserReservationLimit u
            WHERE u.userId = :userId
            """)
    Optional<UserReservationLimit> findByUserIdForUpdate(
            @Param("userId") Long userId);


    @Modifying
    @Query(value = """
        INSERT INTO user_reservation_limits (user_id, confirmed_count)
        VALUES (:userId, 0)
        ON DUPLICATE KEY UPDATE user_id = user_id
        """, nativeQuery = true)
    void createIfNotExists(@Param("userId") Long userId);
}