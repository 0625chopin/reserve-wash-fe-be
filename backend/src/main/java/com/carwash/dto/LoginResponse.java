package com.carwash.dto;

// 인증 응답 — JWT 토큰 + 안전한 사용자 정보(passwordHash 제외)
public record LoginResponse(String token, UserResponse user) {}
