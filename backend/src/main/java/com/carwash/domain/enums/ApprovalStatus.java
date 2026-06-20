package com.carwash.domain.enums;

// 결재 상태 (require 8.3) — 휴무 2단계(SUBMITTED→APPROVED_L1→CONFIRMED)·휴일 1단계(SUBMITTED→CONFIRMED)
//   L2 승인은 곧장 CONFIRMED로 확정(require 8.3 APPROVED_L2는 확정 직전 단계 → 본 구현은 확정으로 collapse).
public enum ApprovalStatus {
    SUBMITTED,
    APPROVED_L1,
    CONFIRMED,
    REJECTED
}
