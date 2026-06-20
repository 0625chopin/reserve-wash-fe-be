package com.carwash.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// 로그인 요청 (require 4장)
public record LoginRequest(
        @Email @NotBlank String email,
        @NotBlank String password) {}
