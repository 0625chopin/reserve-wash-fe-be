package com.carwash.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// 매니저 회원가입 요청 (require v1.9 §4.1) — 소속 매장(storeId) 필수.
//   가입 시 role=MANAGER·approvalStatus=PENDING_APPROVAL_L1로 등록(자동 로그인 없음, 2단계 승인 후 ACTIVE).
public record SignupManagerRequest(
        @Email @NotBlank String email,
        @NotBlank String password,
        @NotBlank String name,
        @NotBlank String storeId) {
}
