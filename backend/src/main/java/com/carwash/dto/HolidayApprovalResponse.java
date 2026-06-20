package com.carwash.dto;

import com.carwash.domain.StoreHoliday;
import com.carwash.domain.enums.ApprovalStatus;

// 매장 휴일 결재 응답 (Phase 7)
public record HolidayApprovalResponse(
        Long id,
        String storeId,
        String date,
        ApprovalStatus status) {

    public static HolidayApprovalResponse from(StoreHoliday h) {
        return new HolidayApprovalResponse(h.getId(), h.getStoreId(), h.getDate(), h.getStatus());
    }
}
