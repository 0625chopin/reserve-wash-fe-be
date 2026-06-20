package com.carwash.dto;

import com.carwash.domain.ManagerDayoff;
import com.carwash.domain.enums.DayoffType;

// 매니저 휴무 응답 DTO — FE ManagerDayoff{date,type}와 무변환 일치
public record DayoffResponse(String date, DayoffType type) {

    public static DayoffResponse from(ManagerDayoff d) {
        return new DayoffResponse(d.getDate(), d.getType());
    }
}
