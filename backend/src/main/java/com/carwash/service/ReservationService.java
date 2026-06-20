package com.carwash.service;

import com.carwash.domain.Reservation;
import com.carwash.domain.Slot;
import com.carwash.domain.enums.ReservationStatus;
import com.carwash.domain.enums.SlotStatus;
import com.carwash.dto.ConfirmRequest;
import com.carwash.dto.HoldRequest;
import com.carwash.dto.ReservationResponse;
import com.carwash.exception.SlotConflictException;
import com.carwash.mapper.ReservationMapper;
import com.carwash.mapper.SlotMapper;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// 예약 서비스 (require 6·7장) — 슬롯 점유(INSERT)·확정(낙관락/비관락)·동시성 처리
//   슬롯 행은 희소(점유 시에만 존재). uk_slot UNIQUE가 동시 INSERT를 1건으로 직렬화하는 최종 방어선.
@Service
public class ReservationService {

    private final SlotMapper slotMapper;
    private final ReservationMapper reservationMapper;

    public ReservationService(SlotMapper slotMapper, ReservationMapper reservationMapper) {
        this.slotMapper = slotMapper;
        this.reservationMapper = reservationMapper;
    }

    // 점유 — INSERT HOLDING. 이미 점유/예약된 슬롯이면 uk 위반(DataIntegrityViolation)→409
    @Transactional
    public void hold(HoldRequest req) {
        slotMapper.insertHold(req.storeId(), req.bayId(), req.date(), req.timeSlot());
    }

    // 확정(낙관락) — INSERT HOLDING → version 비교 UPDATE RESERVED → reservation insert
    @Transactional
    public ReservationResponse confirm(String userId, ConfirmRequest req) {
        // 1) 슬롯 점유(희소 INSERT). 동시 INSERT는 uk_slot UNIQUE가 1건만 통과 → 나머지 409(최종 방어선)
        slotMapper.insertHold(req.storeId(), req.bayId(), req.date(), req.timeSlot());
        // 2) 낙관적 락: HOLDING→RESERVED, 영향 행 수 0이면 다른 트랜잭션 선점 → 충돌
        Slot slot = slotMapper.findByKey(req.storeId(), req.bayId(), req.date(), req.timeSlot());
        int updated = slotMapper.updateStatusWithVersion(slot.getId(), "RESERVED", slot.getVersion());
        if (updated == 0) {
            throw new SlotConflictException("선택하신 슬롯이 방금 예약되었습니다.");
        }
        // 3) 예약 영속(앱이 id 부여)
        Reservation reservation = buildReservation(userId, req);
        reservationMapper.insert(reservation);
        return ReservationResponse.from(reservation);
    }

    // 확정(비관락 시연) — FOR UPDATE로 행 잠금 후 상태 검사·확정(require 7.3 인기 슬롯 경로)
    @Transactional
    public ReservationResponse confirmPessimistic(String userId, ConfirmRequest req) {
        slotMapper.insertHold(req.storeId(), req.bayId(), req.date(), req.timeSlot());
        Slot slot = slotMapper.findForUpdate(req.storeId(), req.bayId(), req.date(), req.timeSlot());
        if (slot.getStatus() == SlotStatus.RESERVED || slot.getStatus() == SlotStatus.COMPLETED) {
            throw new SlotConflictException("선택하신 슬롯이 방금 예약되었습니다.");
        }
        slotMapper.updateStatusWithVersion(slot.getId(), "RESERVED", slot.getVersion());
        Reservation reservation = buildReservation(userId, req);
        reservationMapper.insert(reservation);
        return ReservationResponse.from(reservation);
    }

    // 세차완료(FW6/M4) — RESERVED→COMPLETED + 슬롯 COMPLETED 고정. 불가 전이는 도메인이 예외(→409)
    @Transactional
    public void complete(String userId, String reservationId) {
        Reservation reservation = loadOwned(userId, reservationId);
        reservation.complete();
        reservationMapper.updateStatus(reservation.getId(), reservation.getStatus().name());
        releaseOrComplete(reservation, true);
    }

    // 예약취소(FW7/M5) — RESERVED/HOLDING→CANCELED + 슬롯 AVAILABLE release. 승인 전/후 모두 동일
    @Transactional
    public void cancel(String userId, String reservationId) {
        Reservation reservation = loadOwned(userId, reservationId);
        reservation.cancel();
        reservationMapper.updateStatus(reservation.getId(), reservation.getStatus().name());
        releaseOrComplete(reservation, false);
    }

    // 소유자 검증 — 미존재/타인 예약이면 404(전이 권한 없음 노출 최소화)
    private Reservation loadOwned(String userId, String reservationId) {
        Reservation reservation = reservationMapper.findById(reservationId);
        if (reservation == null || !reservation.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "예약을 찾을 수 없습니다.");
        }
        return reservation;
    }

    // 예약 전이에 종속된 슬롯 상태 갱신 — 완료면 COMPLETED, 취소면 AVAILABLE release(희소 슬롯 단건)
    private void releaseOrComplete(Reservation reservation, boolean completed) {
        Slot slot = slotMapper.findByKey(
                reservation.getStoreId(), reservation.getBayId(),
                reservation.getDate(), reservation.getTimeSlot());
        if (slot == null) {
            return;
        }
        if (completed) {
            slot.complete();
        } else {
            slot.release();
        }
        slotMapper.updateStatusWithVersion(slot.getId(), slot.getStatus().name(), slot.getVersion());
    }

    private Reservation buildReservation(String userId, ConfirmRequest req) {
        return Reservation.builder()
                .id("rsv-" + UUID.randomUUID())
                .userId(userId)
                .storeId(req.storeId())
                .bayId(req.bayId())
                .managerId(req.managerId())
                .date(req.date())
                .timeSlot(req.timeSlot())
                .carType(req.carType())
                .serviceType(req.serviceType())
                .amount(req.amount())
                .status(ReservationStatus.RESERVED)
                .build();
    }
}
