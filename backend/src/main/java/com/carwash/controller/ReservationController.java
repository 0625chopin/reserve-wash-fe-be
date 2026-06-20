package com.carwash.controller;

import com.carwash.dto.ConfirmRequest;
import com.carwash.dto.HoldRequest;
import com.carwash.dto.ReservationResponse;
import com.carwash.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
}
