package com.carwash.dto;

import com.carwash.domain.enums.DayoffType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// 매니저 휴무 결재 상신 요청 (require 5.5·8.2) — 매니저·날짜·휴무유형
public record DayoffRequest(
        @NotBlank String managerId,
        @NotBlank String date,
        @NotNull DayoffType type) {}
