package com.carwash.dto;

// 평균 평점 응답 (require 9.1) — 후기 0건이면 average=0, count=0
public record AverageRatingResponse(double average, int count) {}
