package com.carwash.controller;

import com.carwash.dto.DayoffApprovalResponse;
import com.carwash.dto.DayoffRequest;
import com.carwash.service.ApprovalService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// 매니저 휴무 결재 — 신청/조회/1차 승인/재신청 (require 8.2).
//   /api/manager/** 는 MANAGER·STORE_ADMIN. 단, approve-l1은 SecurityConfig 세분 매처로 STORE_ADMIN 한정.
@RestController
@RequestMapping("/api/manager/dayoffs")
public class DayoffController {

    private final ApprovalService approvalService;

    public DayoffController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    // 휴무 결재 상신 → SUBMITTED
    @PostMapping
    public DayoffApprovalResponse submit(@Valid @RequestBody DayoffRequest req) {
        return approvalService.submitDayoff(req);
    }

    // 매장 매니저들의 휴무 신청 목록(L1 검토용)
    @GetMapping
    public List<DayoffApprovalResponse> list(@RequestParam String storeId) {
        return approvalService.listDayoffsByStore(storeId);
    }

    // 1차 승인(최고매니저 STORE_ADMIN) — SUBMITTED → APPROVED_L1
    @PatchMapping("/{id}/approve-l1")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approveL1(@PathVariable Long id) {
        approvalService.approveDayoffL1(id);
    }

    // 반려 후 재신청 — REJECTED → SUBMITTED
    @PatchMapping("/{id}/resubmit")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resubmit(@PathVariable Long id) {
        approvalService.resubmitDayoff(id);
    }
}
