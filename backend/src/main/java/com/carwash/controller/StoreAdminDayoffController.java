package com.carwash.controller;

import com.carwash.dto.DayoffApprovalResponse;
import com.carwash.service.ApprovalService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// 매장매니저관리자 휴가/반차 결재(M8) — 1단계 승인 종결 (require v1.7 §8.2·§8.3).
//   /api/store-admin/** 는 SecurityConfig 경로 인가로 STORE_ADMIN 한정. 관리자(ADMIN) 개입 없음.
@RestController
@RequestMapping("/api/store-admin/dayoffs")
public class StoreAdminDayoffController {

    private final ApprovalService approvalService;

    public StoreAdminDayoffController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    // 휴가/반차 결재함(전체) — 검토 목록
    @GetMapping
    public List<DayoffApprovalResponse> dayoffs() {
        return approvalService.listAllDayoffs();
    }

    // 1단계 승인(M8) — SUBMITTED → APPROVED 종결
    @PatchMapping("/{id}/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approve(@PathVariable Long id) {
        approvalService.approveDayoff(id);
    }

    // 반려 — SUBMITTED → REJECTED
    @PatchMapping("/{id}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@PathVariable Long id) {
        approvalService.rejectDayoff(id);
    }
}
