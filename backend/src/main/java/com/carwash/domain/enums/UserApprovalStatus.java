package com.carwash.domain.enums;

// 회원가입 승인 상태 (require v1.7 §4.4) — 매니저 계열 2단계 승인.
//   매장매니저관리자 1차(M7): PENDING_APPROVAL_L1 → PENDING_APPROVAL_L2.
//   관리자 2차 최종(S3):     PENDING_APPROVAL_L2 → ACTIVE(로그인 가능 유일 상태).
//   USER는 가입 즉시 ACTIVE 직행(승인 분기 없음). 어느 단계든 거부 시 REJECTED.
//   ※ 이메일 인증 단계(REQUESTED/EMAIL_VERIFIED)는 이연(SMTP 단계에서 도입).
//   ⚠️ 가입=2단계, 휴가/반차(DayoffApprovalStatus)=1단계 — 단계 수가 다름에 유의.
public enum UserApprovalStatus {
    PENDING_APPROVAL_L1,
    PENDING_APPROVAL_L2,
    ACTIVE,
    REJECTED
}
