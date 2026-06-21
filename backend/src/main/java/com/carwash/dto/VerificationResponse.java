package com.carwash.dto;

// 코드 요청/재전송 응답 — FE 카운트다운(3:00) 시작용 만료 잔여 초
public record VerificationResponse(int expiresInSec) {}
