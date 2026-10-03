package com.seat.reservation.service;

import com.seat.reservation.dto.CreateReservationRequest;
import com.seat.reservation.dto.CreateReservationResponse;
import com.seat.reservation.entity.IdempotencyRecord;
import com.seat.reservation.entity.Reservation;
import com.seat.reservation.entity.ReservationSeat;
import com.seat.reservation.entity.ReservationStatus;
import com.seat.reservation.entity.Seat;
import com.seat.reservation.entity.SeatStatus;
import com.seat.reservation.entity.UserReservationLimit;
import com.seat.reservation.exception.ReservationException;
import com.seat.reservation.exception.SeatUnavailableException;
import com.seat.reservation.repository.IdempotencyRecordRepository;
import com.seat.reservation.repository.ReservationRepository;
import com.seat.reservation.repository.ReservationSeatRepository;
import com.seat.reservation.repository.SeatRepository;
import com.seat.reservation.repository.UserReservationLimitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ReservationService {

    private static final Logger logger = LoggerFactory.getLogger(ReservationService.class);

    private static final int MAX_SEATS_PER_RESERVATION = 5;
    private static final int MAX_RESERVATIONS_PER_USER = 5;
    private static final int HOLD_DURATION_MINUTES = 5;

    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationSeatRepository reservationSeatRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final UserReservationLimitRepository userReservationLimitRepository;
    private final ReservationMetricsService reservationMetricsService;

    public ReservationService(SeatRepository seatRepository, ReservationRepository reservationRepository, ReservationSeatRepository reservationSeatRepository, IdempotencyRecordRepository idempotencyRecordRepository, UserReservationLimitRepository userReservationLimitRepository, ReservationMetricsService reservationMetricsService) {
        this.seatRepository = seatRepository;
        this.reservationRepository = reservationRepository;
        this.reservationSeatRepository = reservationSeatRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.userReservationLimitRepository = userReservationLimitRepository;
        this.reservationMetricsService = reservationMetricsService;
    }

    @Transactional
    public CreateReservationResponse createReservation(
            CreateReservationRequest request,
            String idempotencyKey) {

        validateRequest(request, idempotencyKey);

        String requestHash = generateRequestHash(request);

        IdempotencyRecord existingRecord =
                idempotencyRecordRepository
                        .findByIdempotencyKeyForUpdate(idempotencyKey)
                        .orElse(null);

        if (existingRecord != null) {
            return handleExistingRequest(existingRecord, requestHash);
        }

        validateUserReservationLimit(request.getUserId());

        List<Long> seatIds =
                new ArrayList<>(request.getSeatIds());

        Collections.sort(seatIds);

        List<Seat> seats =
                seatRepository.findAllByIdForUpdate(seatIds);

        validateSeats(seatIds, seats);

        Reservation reservation =
                createReservationEntity(request);

        Reservation savedReservation =
                reservationRepository.save(reservation);

        saveReservationSeats(savedReservation, seats);

        IdempotencyRecord idempotencyRecord =
                createIdempotencyRecord(
                        idempotencyKey,
                        requestHash,
                        savedReservation.getId());

        try {
            idempotencyRecordRepository.save(idempotencyRecord);
        } catch (DataIntegrityViolationException exception) {

            IdempotencyRecord existingRecords =
                    idempotencyRecordRepository
                            .findByIdempotencyKey(idempotencyKey)
                            .orElseThrow(() ->
                                    new ReservationException(
                                            "Unable to process idempotent request"));

            return handleExistingRequest(existingRecords, requestHash);
        }

        logger.info(
                "Reservation created: reservationId={}, userId={}, seatIds={}, status={}",
                savedReservation.getId(),
                savedReservation.getUserId(),
                seatIds,
                savedReservation.getStatus());

        return buildResponse(savedReservation, seatIds);

    }

    @Transactional
    public CreateReservationResponse confirmReservation(
            Long reservationId) {

        Reservation reservation =
                reservationRepository.findByIdForUpdate(reservationId)
                        .orElseThrow(() ->
                                new ReservationException(
                                        "Reservation not found"));

        if (reservation.getStatus() != ReservationStatus.HELD) {
            throw new ReservationException(
                    "Only held reservations can be confirmed");
        }

        if (reservation.getHoldExpiresAt() != null
                && reservation.getHoldExpiresAt()
                .isBefore(LocalDateTime.now())) {

            throw new ReservationException(
                    "Reservation hold has expired");
        }

        UserReservationLimit userReservationLimit =
                lockUserReservationLimit(reservation.getUserId());

        if (userReservationLimit.getConfirmedCount()
                >= MAX_RESERVATIONS_PER_USER) {

            throw new ReservationException(
                    "User has reached the maximum reservation limit");
        }

        List<ReservationSeat> reservationSeats =
                reservationSeatRepository
                        .findByReservationId(reservationId);


        for (ReservationSeat reservationSeat : reservationSeats) {

            Seat seat = reservationSeat.getSeat();

            if (seat.getStatus() != SeatStatus.HELD) {
                throw new SeatUnavailableException(
                        "Seat " + seat.getSeatNumber()
                                + " is no longer held");
            }

            seat.setStatus(SeatStatus.RESERVED);
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setHoldExpiresAt(null);

        reservationRepository.save(reservation);

        incrementUserReservationCount(userReservationLimit);
        reservationMetricsService.reservationConfirmed();

        logger.info(
                "Reservation confirmed: reservationId={}, userId={}, seatIds={}",
                reservation.getId(),
                reservation.getUserId(),
                reservationSeats.stream()
                        .map(reservationSeat ->
                                reservationSeat.getSeat().getSeatNumber())
                        .sorted()
                        .toList());

        List<Long> seatIds =
                reservationSeats.stream()
                        .map(reservationSeat ->
                                reservationSeat.getSeat().getId())
                        .sorted()
                        .toList();

        return buildResponse(reservation, seatIds);
    }

    private void validateRequest(
            CreateReservationRequest request,
            String idempotencyKey) {

        if (request == null) {
            throw new ReservationException(
                    "Request cannot be null");
        }

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ReservationException(
                    "Idempotency-Key header is required");
        }

        if (request.getSeatIds() == null
                || request.getSeatIds().isEmpty()) {

            throw new ReservationException(
                    "At least one seat is required");
        }

        if (request.getSeatIds().size()
                > MAX_SEATS_PER_RESERVATION) {

            throw new ReservationException(
                    "Maximum "
                            + MAX_SEATS_PER_RESERVATION
                            + " seats can be reserved in one request");
        }
    }

    private void validateUserReservationLimit(Long userId) {

        UserReservationLimit userReservationLimit =
                lockUserReservationLimit(userId);

        if (userReservationLimit.getConfirmedCount()
                >= MAX_RESERVATIONS_PER_USER) {

            logger.warn(
                    "Reservation declined: userId={}, reason=per-user-limit, confirmedCount={}",
                    userId,
                    userReservationLimit.getConfirmedCount());

            reservationMetricsService
                    .reservationDeclinedUserLimit();

            throw new ReservationException(
                    "User has reached the maximum reservation limit");
        }
    }

    private UserReservationLimit lockUserReservationLimit(
            Long userId) {

        userReservationLimitRepository.createIfNotExists(userId);

        return userReservationLimitRepository
                .findByUserIdForUpdate(userId)
                .orElseThrow(() ->
                        new ReservationException(
                                "Unable to initialize user reservation limit"));
    }

    private void incrementUserReservationCount(
            UserReservationLimit userReservationLimit) {

        userReservationLimit.setConfirmedCount(
                userReservationLimit.getConfirmedCount() + 1);

        userReservationLimitRepository.save(
                userReservationLimit);
    }

    private CreateReservationResponse handleExistingRequest(
            IdempotencyRecord existingRecord,
            String requestHash) {

        if (!existingRecord.getRequestHash().equals(requestHash)) {
            throw new ReservationException(
                    "Idempotency-Key has already been used with a different request");
        }

        Reservation reservation =
                reservationRepository
                        .findById(existingRecord.getReservationId())
                        .orElseThrow(() ->
                                new ReservationException(
                                        "Reservation associated with idempotency key was not found"));

        List<Long> seatIds =
                reservationSeatRepository
                        .findByReservationId(reservation.getId())
                        .stream()
                        .map(reservationSeat ->
                                reservationSeat.getSeat().getId())
                        .sorted()
                        .toList();

        logger.info(
                "Idempotent replay: idempotencyKey={}, reservationId={}",
                existingRecord.getIdempotencyKey(),
                existingRecord.getReservationId());

        reservationMetricsService
                .reservationDeclinedIdempotentReplay();

        return buildResponse(reservation, seatIds);
    }

    private void validateSeats(
            List<Long> requestedSeatIds,
            List<Seat> seats) {

        if (seats.size() != requestedSeatIds.size()) {
            throw new SeatUnavailableException(
                    "One or more requested seats do not exist");
        }

        for (Seat seat : seats) {

            if (seat.getStatus() != SeatStatus.AVAILABLE) {

                logger.warn(
                        "Reservation declined: seatId={}, seatNumber={}, reason=seat-taken",
                        seat.getId(),
                        seat.getSeatNumber());

                reservationMetricsService
                        .reservationDeclinedSeatTaken();

                throw new SeatUnavailableException(
                        "Seat " + seat.getSeatNumber()
                                + " is not available");
            }
        }
    }

    private Reservation createReservationEntity(
            CreateReservationRequest request) {

        Reservation reservation = new Reservation();

        reservation.setUserId(request.getUserId());
        reservation.setStatus(ReservationStatus.HELD);
        reservation.setTotalAmountPaise(
                request.getTotalAmountPaise());

        reservation.setHoldExpiresAt(
                LocalDateTime.now()
                        .plusMinutes(HOLD_DURATION_MINUTES));

        return reservation;
    }

    private void saveReservationSeats(
            Reservation reservation,
            List<Seat> seats) {

        for (Seat seat : seats) {

            seat.setStatus(SeatStatus.HELD);

            ReservationSeat reservationSeat =
                    new ReservationSeat();

            reservationSeat.setReservation(reservation);
            reservationSeat.setSeat(seat);

            reservationSeatRepository.save(reservationSeat);
        }
    }

    private IdempotencyRecord createIdempotencyRecord(
            String idempotencyKey,
            String requestHash,
            Long reservationId) {

        IdempotencyRecord record =
                new IdempotencyRecord();

        record.setIdempotencyKey(idempotencyKey);
        record.setRequestHash(requestHash);
        record.setReservationId(reservationId);
        record.setResponseStatus(201);

        return record;
    }

    private CreateReservationResponse buildResponse(
            Reservation reservation,
            List<Long> seatIds) {

        return new CreateReservationResponse(
                reservation.getId(),
                reservation.getUserId(),
                seatIds,
                reservation.getStatus(),
                reservation.getTotalAmountPaise());
    }

    private String generateRequestHash(
            CreateReservationRequest request) {

        List<Long> sortedSeatIds =
                new ArrayList<>(request.getSeatIds());

        Collections.sort(sortedSeatIds);

        String requestData =
                request.getUserId()
                        + "|"
                        + sortedSeatIds
                        + "|"
                        + request.getTotalAmountPaise();

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            requestData.getBytes(
                                    StandardCharsets.UTF_8));

            StringBuilder result =
                    new StringBuilder();

            for (byte value : hash) {
                result.append(
                        String.format("%02x", value));
            }

            return result.toString();

        } catch (NoSuchAlgorithmException exception) {
            throw new ReservationException(
                    "Unable to generate request hash");
        }
    }


    public Reservation getReservation(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() ->
                        new ReservationException("Reservation not found"));
    }
}