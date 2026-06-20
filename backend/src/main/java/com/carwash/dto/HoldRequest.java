package com.carwash.dto;

import jakarta.validation.constraints.NotBlank;

// 슬롯 점유 요청 (require 6장·7장)
public record HoldRequest(
        @NotBlank String storeId,
        @NotBlank String bayId,
        @NotBlank String date,
        @NotBlank String timeSlot) {}
