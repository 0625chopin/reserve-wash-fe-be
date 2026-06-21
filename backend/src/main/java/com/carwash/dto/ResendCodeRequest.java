package com.carwash.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// 인증 코드 재전송 — 만료/미수신 시 새 코드 발급
public record ResendCodeRequest(@Email @NotBlank String email) {}
