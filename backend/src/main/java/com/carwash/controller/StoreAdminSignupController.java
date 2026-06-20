package com.carwash.controller;

import com.carwash.dto.ManagerSignupResponse;
import com.carwash.service.SignupApprovalService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// 매장매니저관리자 매니저 가입 1차 승인(M7) (require v1.7 §4.4·§11.2).
//   /api/store-admin/** 는 SecurityConfig 경로 인가로 STORE_ADMIN 한정.
@RestController
@RequestMapping("/api/store-admin/manager-signups")
public class StoreAdminSignupController {

    private final SignupApprovalService signupApprovalService;

    public StoreAdminSignupController(SignupApprovalService signupApprovalService) {
        this.signupApprovalService = signupApprovalService;
    }

    // 1차 승인 대기 목록(PENDING_APPROVAL_L1)
    @GetMapping
    public List<ManagerSignupResponse> pending() {
        return signupApprovalService.listPendingL1();
    }

    // 1차 승인(M7) — PENDING_APPROVAL_L1 → PENDING_APPROVAL_L2
    @PatchMapping("/{id}/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approve(@PathVariable String id) {
        signupApprovalService.approveL1(id);
    }

    // 반려 — → REJECTED
    @PatchMapping("/{id}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@PathVariable String id) {
        signupApprovalService.reject(id);
    }
}
