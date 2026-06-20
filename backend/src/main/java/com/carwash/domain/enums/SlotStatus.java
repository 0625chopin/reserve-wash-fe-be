package com.carwash.domain.enums;

// 슬롯 상태 — 동시성 처리의 핵심 (require 7.1) — FE SlotStatus 리터럴과 글자까지 일치
public enum SlotStatus {
    AVAILABLE,   // 예약 가능
    HOLDING,     // 점유(확정 전)
    RESERVED,    // 예약 확정
    COMPLETED    // 세차 완료
}
