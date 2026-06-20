package com.carwash.domain.enums;

// 예약 상태 전이 (require 11.3) — FE ReservationStatus 리터럴과 글자까지 일치
public enum ReservationStatus {
    HOLDING,     // 슬롯 점유(확정 전)
    RESERVED,    // 예약 확정
    COMPLETED,   // 세차 완료
    CANCELED     // 취소됨
}
