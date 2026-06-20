package com.carwash.domain.enums;

// 휴가/반차(매니저 휴무) 결재 상태 — 1단계 승인 (require v1.7 §8.2·§8.3)
//   일반매장매니저 신청(M6, SUBMITTED) → 매장매니저관리자(STORE_ADMIN)가 APPROVED로 종결(M8).
//   관리자(ADMIN) 개입 없음. APPROVED = 확정(카탈로그 노출 → 슬롯 비활성 반영).
//   ※ 가입 2단계 승인(UserApprovalStatus)·휴일 1단계 승인(ApprovalStatus)과 물리적으로 분리하여
//     단계 수 혼동을 코드 레벨에서 차단한다.
public enum DayoffApprovalStatus {
    SUBMITTED,
    APPROVED,
    REJECTED
}
