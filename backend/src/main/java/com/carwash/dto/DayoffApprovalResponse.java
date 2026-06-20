package com.carwash.dto;

import com.carwash.domain.ManagerDayoff;
import com.carwash.domain.enums.ApprovalStatus;
import com.carwash.domain.enums.DayoffType;

// 매니저 휴무 결재 응답 (Phase 7) — 카탈로그용 DayoffResponse와 구분(결재 메타 포함)
public record DayoffApprovalResponse(
        Long id,
        String managerId,
        String date,
        DayoffType type,
        ApprovalStatus status) {

    public static DayoffApprovalResponse from(ManagerDayoff d) {
        return new DayoffApprovalResponse(
                d.getId(), d.getManagerId(), d.getDate(), d.getType(), d.getStatus());
    }
}
