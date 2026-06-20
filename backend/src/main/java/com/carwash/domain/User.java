package com.carwash.domain;

import com.carwash.domain.enums.UserApprovalStatus;
import com.carwash.domain.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 사용자 (require 3.1) — 순수 POJO, JPA 애너테이션 없음
// 필드명은 FE app/types/domain.ts User와 무변환 일치(id는 VARCHAR 문자열, Phase 0 id 정책)
@Getter
@NoArgsConstructor              // MyBatis 결과 매핑용 기본 생성자
@AllArgsConstructor
@Builder
public class User {

    private String id;          // 'user1' 등 문자열 id (무변환)
    private String email;
    private String name;
    private UserRole role;
    private String passwordHash;          // BCrypt 해시 (Phase 3 인증) — 응답 DTO에는 절대 노출 금지
    private UserApprovalStatus approvalStatus;   // 가입 승인 상태 (require v1.7 §4.4) — ACTIVE만 로그인 가능
    private String storeId;               // 매니저 계열 소속 매장(USER/ADMIN은 null) — BO 매장 고정용

    // 가입 1차 승인(M7) — 매장매니저관리자(STORE_ADMIN): PENDING_APPROVAL_L1 → PENDING_APPROVAL_L2.
    //   불가 전이 시 IllegalStateException → 409(GlobalExceptionHandler).
    public void approveSignupL1() {
        if (this.approvalStatus != UserApprovalStatus.PENDING_APPROVAL_L1) {
            throw new IllegalStateException("가입 1차 승인 불가 상태: " + this.approvalStatus);
        }
        this.approvalStatus = UserApprovalStatus.PENDING_APPROVAL_L2;
    }

    // 가입 2차 최종 승인(S3) — 관리자(ADMIN): PENDING_APPROVAL_L2 → ACTIVE(로그인 가능).
    public void confirmSignupL2() {
        if (this.approvalStatus != UserApprovalStatus.PENDING_APPROVAL_L2) {
            throw new IllegalStateException("가입 2차 승인 불가 상태: " + this.approvalStatus);
        }
        this.approvalStatus = UserApprovalStatus.ACTIVE;
    }

    // 가입 거부 — 1차/2차 어느 단계든 검토자가 반려: → REJECTED.
    public void rejectSignup() {
        if (this.approvalStatus != UserApprovalStatus.PENDING_APPROVAL_L1
                && this.approvalStatus != UserApprovalStatus.PENDING_APPROVAL_L2) {
            throw new IllegalStateException("가입 반려 불가 상태: " + this.approvalStatus);
        }
        this.approvalStatus = UserApprovalStatus.REJECTED;
    }
}
