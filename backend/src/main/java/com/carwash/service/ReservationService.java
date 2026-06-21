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
import java.util.List;
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

    // 본인 예약 목록(FW6 require 6.1) — userId 소유 예약 전체. 매니저 대행 예약(userId=고객)도 포함된다.
    //   userId는 호출부에서 JWT principal(uid)로만 도출(요청 바디/쿼리로 받지 않음).
    @Transactional(readOnly = true)
    public List<ReservationResponse> listMine(String userId) {
        return reservationMapper.findByUser(userId).stream().map(ReservationResponse::from).toList();
    }

    // 담당 매니저 예약 목록(require v1.10 §6.6) — managerId(manager 엔티티 id) 귀속 예약 전체.
    //   사용자가 그 매니저를 지정한 일반 예약 + 매니저 대행 예약이 모두 포함된다. managerId는 호출부가 로그인 계정에서 해석.
    @Transactional(readOnly = true)
    public List<ReservationResponse> listByManagerId(String managerId) {
        return reservationMapper.findByManager(managerId).stream().map(ReservationResponse::from).toList();
    }

    // 매장 전체 예약 목록(require v1.10 §6.6 STORE_ADMIN) — storeId 귀속 예약 전체(매장 내 모든 매니저 예약 건).
    @Transactional(readOnly = true)
    public List<ReservationResponse> listByStoreId(String storeId) {
        return reservationMapper.findByStore(storeId).stream().map(ReservationResponse::from).toList();
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
