package com.carwash.controller;

import com.carwash.dto.AdminReservationResponse;
import com.carwash.dto.AdminUserResponse;
import com.carwash.service.AdminService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 관리자 매장 관리 (S4·S5) — /api/admin/** 는 SecurityConfig 경로 인가(ADMIN)로 보호.
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // S4 — 매장별 예약자 관리
    @GetMapping("/stores/{id}/reservations")
    public List<AdminReservationResponse> reservations(@PathVariable String id) {
        return adminService.storeReservations(id);
    }

    // S5 — 매장별 사용자 관리
    @GetMapping("/stores/{id}/users")
    public List<AdminUserResponse> users(@PathVariable String id) {
        return adminService.storeUsers(id);
    }
}
