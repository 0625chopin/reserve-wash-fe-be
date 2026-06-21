package com.carwash.controller;

import com.carwash.dto.ConfirmRequest;
import com.carwash.dto.HoldRequest;
import com.carwash.dto.ReservationResponse;
import com.carwash.service.ReservationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// 예약 진입점 (require 6·7장) — 보호 API(JWT 필수). userId는 토큰(uid)에서 도출.
//   /api/reservations/** 는 SecurityConfig anyRequest().authenticated()로 자동 보호.
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    // 본인 예약 목록(FW6 require 6.1) — JWT uid 소유 예약 전체(대행 예약 포함). userId는 토큰(uid)에서 도출.
    @GetMapping
    public List<ReservationResponse> listMine(@AuthenticationPrincipal String userId) {
        return reservationService.listMine(userId);
    }

    // 슬롯 점유 — INSERT HOLDING(충돌 시 409)
    @PostMapping("/hold")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void hold(@Valid @RequestBody HoldRequest request) {
        reservationService.hold(request);
    }

    // 예약 확정(낙관락) — 충돌 시 409. userId는 JWT principal(uid)에서
    @PostMapping("/confirm")
    public ReservationResponse confirm(
            @AuthenticationPrincipal String userId, @Valid @RequestBody ConfirmRequest request) {
        return reservationService.confirm(userId, request);
    }

    // 세차완료(FW6/M4) — RESERVED→COMPLETED. 불가 전이 409, 비소유 404
    @PatchMapping("/{id}/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void complete(@AuthenticationPrincipal String userId, @PathVariable String id) {
        reservationService.complete(userId, id);
    }

    // 예약취소(FW7/M5) — RESERVED/HOLDING→CANCELED + 슬롯 release. 불가 전이 409, 비소유 404
    @PatchMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@AuthenticationPrincipal String userId, @PathVariable String id) {
        reservationService.cancel(userId, id);
    }

    // 예약 승인(M6)은 Phase 0 결정에 따라 미도입(1차 parity) — Phase 6 BO 대행과 함께 이연
}
