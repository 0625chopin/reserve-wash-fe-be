package com.carwash.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// 회원가입 요청 (FW1, require 4.3) — 일반 사용자(USER) 즉시 가입
public record SignupRequest(
        @Email @NotBlank String email,
        @NotBlank String password,
        @NotBlank String name) {}
