package com.carwash.dto;

import com.carwash.domain.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// 관리자 직접 매니저 등록 요청 (require v1.12 §4.1) — 역할(MANAGER|STORE_ADMIN) 선택 + 소속 매장 지정.
//   생성 시 approvalStatus=PENDING_APPROVAL_L2(2차 최종 승인 대기) — 1차(매장매니저관리자) 생략, 관리자 최종 승인 후 ACTIVE.
public record AdminCreateManagerRequest(
        @Email @NotBlank String email,
        @NotBlank String password,
        @NotBlank String name,
        @NotBlank String storeId,
        @NotNull UserRole role) {
}
