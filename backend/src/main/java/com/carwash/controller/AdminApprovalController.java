package com.carwash.controller;

import com.carwash.dto.DayoffApprovalResponse;
import com.carwash.dto.HolidayApprovalResponse;
import com.carwash.service.ApprovalService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// 관리자 결재함 (require 8.2·8.3) — 휴무 2차 승인/반려, 휴일 단일 승인/반려.
//   /api/admin/** 는 SecurityConfig 경로 인가로 ADMIN 한정.
@RestController
@RequestMapping("/api/admin")
public class AdminApprovalController {

    private final ApprovalService approvalService;

    public AdminApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    // 휴무 결재함(전체)
    @GetMapping("/dayoffs")
    public List<DayoffApprovalResponse> dayoffs() {
        return approvalService.listAllDayoffs();
    }

    // 2차 승인(관리자) — APPROVED_L1 → CONFIRMED
    @PatchMapping("/dayoffs/{id}/approve-l2")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approveDayoffL2(@PathVariable Long id) {
        approvalService.approveDayoffL2(id);
    }

    @PatchMapping("/dayoffs/{id}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejectDayoff(@PathVariable Long id) {
        approvalService.rejectDayoff(id);
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
}
