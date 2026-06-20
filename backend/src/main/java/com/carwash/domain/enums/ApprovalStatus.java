package com.carwash.domain.enums;

// 매장 휴일 결재 상태 (require v1.7 §8.1) — 1단계 승인(SUBMITTED → CONFIRMED / REJECTED).
//   ⚠️ 휴가/반차(휴무)는 v1.7에서 DayoffApprovalStatus(1단계, STORE_ADMIN 종결)로 분리됨 → 본 enum은 휴일 전용.
public enum ApprovalStatus {
    SUBMITTED,
    CONFIRMED,
    REJECTED
}
