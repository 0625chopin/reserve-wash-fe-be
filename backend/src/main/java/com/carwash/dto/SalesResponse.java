package com.carwash.dto;

// 매장별 매출 집계 응답 (S8, require 11.1) — COMPLETED 예약 금액 합산
public record SalesResponse(String storeId, long total) {}
