package com.carwash.dto;

import com.carwash.domain.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// 인증 코드 요청 (가입 1단계) — 가입 정보를 받아 6자리 코드를 발송한다.
//   role=USER(일반) 또는 MANAGER(매니저). 매니저는 storeId 필수(서비스에서 검증).
public record SignupCodeRequest(
        @Email @NotBlank String email,
        @NotBlank String password,
        @NotBlank String name,
        @NotNull UserRole role,
        String storeId) {}
