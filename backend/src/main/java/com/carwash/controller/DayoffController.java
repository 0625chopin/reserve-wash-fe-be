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

// 매니저 휴가/반차 신청(M6) — 신청/조회/재신청 (require v1.7 §8.2).
//   /api/manager/** 는 MANAGER·STORE_ADMIN 접근. 승인(M8)은 STORE_ADMIN 전용 StoreAdminDayoffController가 담당.
@RestController
@RequestMapping("/api/manager/dayoffs")
public class DayoffController {

    private final ApprovalService approvalService;

    public DayoffController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    // 휴가/반차 신청 상신(M6) → SUBMITTED
    @PostMapping
    public DayoffApprovalResponse submit(@Valid @RequestBody DayoffRequest req) {
        return approvalService.submitDayoff(req);
    }

    // 매장 매니저들의 휴가/반차 신청 목록(신청 현황 조회)
    @GetMapping
    public List<DayoffApprovalResponse> list(@RequestParam String storeId) {
        return approvalService.listDayoffsByStore(storeId);
    }

    // 반려 후 재신청(신청자) — REJECTED → SUBMITTED
    @PatchMapping("/{id}/resubmit")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resubmit(@PathVariable Long id) {
        approvalService.resubmitDayoff(id);
    }
}
