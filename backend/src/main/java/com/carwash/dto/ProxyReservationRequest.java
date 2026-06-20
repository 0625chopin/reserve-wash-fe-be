package com.carwash.dto;

import com.carwash.domain.enums.CarType;
import com.carwash.domain.enums.ServiceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// 매니저 대행 예약 요청 (M3, require 6.2) — 대행자는 JWT 역할로 인가되고,
//   대행 매니저(managerId)는 본문에서 받아 소속 매장·휴무를 서버가 검증한다.
//   고객은 이메일로 식별(서버가 email→userId 해석).
public record ProxyReservationRequest(
        @NotBlank String customerEmail,    // 대행 대상 고객 이메일
        @NotBlank String managerId,        // 대행 매니저 id(소속 매장 검증 기준)
        @NotBlank String storeId,
        @NotBlank String bayId,
        @NotBlank String date,
        @NotBlank String timeSlot,
        @NotNull CarType carType,
        @NotNull ServiceType serviceType,
        int amount) {}
