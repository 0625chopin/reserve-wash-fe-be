package com.carwash.controller;

import com.carwash.dto.ReservationResponse;
import com.carwash.service.ManagerReservationService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 매장매니저관리자 예약 목록 (require v1.10 §6.6) — /api/store-admin/** 는 SecurityConfig 경로 인가(STORE_ADMIN 한정)로 보호.
//   매장 전체 예약(매장 내 모든 매니저 예약 건)을 storeId 기준으로 조회한다.
@RestController
@RequestMapping("/api/store-admin/reservations")
public class StoreAdminReservationController {

    private final ManagerReservationService service;

    public StoreAdminReservationController(ManagerReservationService service) {
        this.service = service;
    }

    // 매장 전체 예약 목록 — 로그인 STORE_ADMIN(uid)→store_id 해석 후 매장 귀속 예약 전체 조회.
    @GetMapping
    public List<ReservationResponse> listStoreReservations(@AuthenticationPrincipal String userId) {
        return service.listStoreReservations(userId);
    }
}
