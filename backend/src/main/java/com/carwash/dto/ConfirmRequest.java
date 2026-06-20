package com.carwash.dto;

import com.carwash.domain.enums.CarType;
import com.carwash.domain.enums.ServiceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// 예약 확정 요청 (require 6장) — userId는 바디에서 받지 않고 JWT 토큰(uid)에서 도출
public record ConfirmRequest(
        @NotBlank String storeId,
        @NotBlank String bayId,
        @NotBlank String date,
        @NotBlank String timeSlot,
        String managerId,                 // 매니저 대행이 아니면 null
        @NotNull CarType carType,
        @NotNull ServiceType serviceType,
        int amount) {}
