package com.carwash.controller;

import com.carwash.dto.ProxyReservationRequest;
import com.carwash.dto.ReservationResponse;
import com.carwash.service.ManagerReservationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 매니저 대행 예약 (M3) — /api/manager/** 는 SecurityConfig 경로 인가(MANAGER/STORE_ADMIN)로 보호.
@RestController
@RequestMapping("/api/manager/reservations")
public class ManagerReservationController {

    private final ManagerReservationService service;

    public ManagerReservationController(ManagerReservationService service) {
        this.service = service;
    }

    // 담당 예약 목록(require v1.10 §6.6) — 로그인 매니저(uid)→manager_id 해석 후 담당 예약 조회.
    //   본인 예약(GET /api/reservations)과 분리된 목록. userId는 JWT principal(uid)에서만 도출.
    @GetMapping
    public List<ReservationResponse> listAssigned(@AuthenticationPrincipal String userId) {
        return service.listAssignedToManager(userId);
    }

    // 대행 예약 — 소속 매장·본인 휴무 검증 후 confirm 위임(동시성 동일). 충돌 시 409.
    @PostMapping
    public ReservationResponse proxyReserve(@Valid @RequestBody ProxyReservationRequest req) {
        return service.proxyReserve(req);
    }
}
