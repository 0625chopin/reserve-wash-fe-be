package com.carwash.domain;

import com.carwash.domain.enums.DayoffType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 매니저 휴무 = (날짜, 휴무 유형) (require 5.4·5.5) — Manager.dayoffs 의 원소
// FE ManagerDayoff{date,type}와 무변환 일치. (managerId는 DB FK로만 존재, FE 미노출)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManagerDayoff {

    private String date;        // 'YYYY-MM-DD'
    private DayoffType type;    // FULL_DAY=전일 / SHIFT_1~3=교대조 단위
}
