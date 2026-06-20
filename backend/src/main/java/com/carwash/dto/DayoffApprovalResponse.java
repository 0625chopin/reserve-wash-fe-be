package com.carwash.dto;

import com.carwash.domain.ManagerDayoff;
import com.carwash.domain.enums.DayoffApprovalStatus;
import com.carwash.domain.enums.DayoffType;

// 매니저 휴가/반차 결재 응답 (M6 신청·M8 승인) — 카탈로그용 DayoffResponse와 구분(결재 메타 포함)
public record DayoffApprovalResponse(
        Long id,
        String managerId,
        String date,
        DayoffType type,
        DayoffApprovalStatus status) {

    public static DayoffApprovalResponse from(ManagerDayoff d) {
        return new DayoffApprovalResponse(
                d.getId(), d.getManagerId(), d.getDate(), d.getType(), d.getStatus());
    }
}
