package com.carwash.dto;

import jakarta.validation.constraints.NotBlank;

// 매장 휴일 결재 상신 요청 (require 8.1) — 매장·날짜
public record HolidayRequest(
        @NotBlank String storeId,
        @NotBlank String date) {}
