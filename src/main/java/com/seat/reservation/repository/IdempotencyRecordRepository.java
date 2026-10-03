package com.seat.reservation.repository;

import com.seat.reservation.entity.IdempotencyRecord;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IdempotencyRecordRepository
        extends JpaRepository<IdempotencyRecord, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT i
        FROM IdempotencyRecord i
        WHERE i.idempotencyKey = :idempotencyKey
        """)
    Optional<IdempotencyRecord> findByIdempotencyKeyForUpdate(
            @Param("idempotencyKey") String idempotencyKey);

    Optional<IdempotencyRecord> findByIdempotencyKey(
            String idempotencyKey);
}