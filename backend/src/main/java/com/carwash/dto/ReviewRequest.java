package com.carwash.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

// 후기 작성 요청 (require 9.1) — reservationId는 BE 예약 id. 평점 1~5(Bean Validation).
public record ReviewRequest(
        @NotBlank String reservationId,
        @Min(1) @Max(5) int rating,
        String text) {}
