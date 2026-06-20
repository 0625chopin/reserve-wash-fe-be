package com.carwash.dto;

import com.carwash.domain.Slot;
import com.carwash.domain.enums.SlotStatus;

// 슬롯 점유 상태 응답 — FE 그리드 하이드레이트(희소: 점유된 슬롯만)
public record SlotStatusResponse(
        String storeId, String bayId, String date, String timeSlot, SlotStatus status) {

    public static SlotStatusResponse from(Slot s) {
        return new SlotStatusResponse(
                s.getStoreId(), s.getBayId(), s.getDate(), s.getTimeSlot(), s.getStatus());
    }
}
