package com.carwash.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// 인증 코드 검증 (가입 2단계) — 6자리 숫자 코드
public record VerifyCodeRequest(
        @Email @NotBlank String email,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "6자리 숫자 코드여야 합니다.") String code) {}
