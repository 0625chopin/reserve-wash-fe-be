package com.carwash.dto;

// 일관 에러 응답 (require 7.3 — 409/404/400)
public record ErrorResponse(String code, String message) {}
