package com.carwash.domain.enums;

// 매니저 휴무 유형 (require 5.5) — 운영 24시간 유지, 근무 3교대 기반 부분 휴무
//   FULL_DAY=전일 휴무 / SHIFT_1(오전 06:00~14:00) / SHIFT_2(오후 14:00~22:00) / SHIFT_3(야간 22:00~06:00 익일)
// FE DayoffType 리터럴과 글자까지 일치
public enum DayoffType {
    FULL_DAY,
    SHIFT_1,
    SHIFT_2,
    SHIFT_3
}
