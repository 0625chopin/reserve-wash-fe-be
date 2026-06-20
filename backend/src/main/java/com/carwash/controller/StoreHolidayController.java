package com.carwash.controller;

import com.carwash.dto.HolidayApprovalResponse;
import com.carwash.dto.HolidayRequest;
import com.carwash.service.ApprovalService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 매장 휴일 결재 상신 (require 8.1) — 매니저 신청(/api/manager/** = MANAGER·STORE_ADMIN). 승인은 관리자(AdminApprovalController).
@RestController
@RequestMapping("/api/manager/holidays")
public class StoreHolidayController {

    private final ApprovalService approvalService;

    public StoreHolidayController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @PostMapping
    public HolidayApprovalResponse submit(@Valid @RequestBody HolidayRequest req) {
        return approvalService.submitHoliday(req);
    }
}
