package com.carwash.controller;

import com.carwash.dto.HolidayApprovalResponse;
import com.carwash.dto.ManagerSignupResponse;
import com.carwash.service.ApprovalService;
import com.carwash.service.SignupApprovalService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// 관리자 결재함 (require v1.7 §8.1·§4.4) — 매장 휴일 단일 승인 + 매니저 가입 2차 최종 승인(S3).
//   ⚠️ 휴가/반차(휴무)는 v1.7에서 1단계(매장매니저관리자 종결)로 정정되어 관리자 개입 없음 → StoreAdminDayoffController 담당.
//   /api/admin/** 는 SecurityConfig 경로 인가로 ADMIN 한정.
@RestController
@RequestMapping("/api/admin")
public class AdminApprovalController {

    private final ApprovalService approvalService;
    private final SignupApprovalService signupApprovalService;

    public AdminApprovalController(
            ApprovalService approvalService, SignupApprovalService signupApprovalService) {
        this.approvalService = approvalService;
        this.signupApprovalService = signupApprovalService;
    }

    // 휴일 결재함(전체)
    @GetMapping("/holidays")
    public List<HolidayApprovalResponse> holidays() {
        return approvalService.listHolidays();
    }

    // 휴일 단일 승인(관리자) — SUBMITTED → CONFIRMED
    @PatchMapping("/holidays/{id}/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approveHoliday(@PathVariable Long id) {
        approvalService.approveHoliday(id);
    }

    @PatchMapping("/holidays/{id}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejectHoliday(@PathVariable Long id) {
        approvalService.rejectHoliday(id);
    }

    // ── 매니저 가입 2차 최종 승인(S3) ───────────────────────────────────
    // 2차 승인 대기 목록(PENDING_APPROVAL_L2)
    @GetMapping("/manager-approvals")
    public List<ManagerSignupResponse> managerApprovals() {
        return signupApprovalService.listPendingL2();
    }

    // 2차 최종 승인(S3) — PENDING_APPROVAL_L2 → ACTIVE
    @PatchMapping("/manager-approvals/{id}/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmManager(@PathVariable String id) {
        signupApprovalService.confirmL2(id);
    }

    // 반려 — → REJECTED
    @PatchMapping("/manager-approvals/{id}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejectManager(@PathVariable String id) {
        signupApprovalService.reject(id);
    }
}
