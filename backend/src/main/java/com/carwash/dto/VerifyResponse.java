package com.carwash.dto;

// 코드 검증 성공 응답 — USER 는 token+user(자동 로그인), 매니저 계열은 pendingApproval=true(승인 대기).
public record VerifyResponse(String token, UserResponse user, boolean pendingApproval) {}
