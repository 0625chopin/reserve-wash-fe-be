package com.carwash.dto;

import com.carwash.domain.Review;

// 후기 응답 DTO — FE Review와 무변환 일치
public record ReviewResponse(
        String id,
        String reservationId,
        String userId,
        String storeId,
        String managerId,
        int rating,
        String text,
        String createdAt) {

    public static ReviewResponse from(Review r) {
        return new ReviewResponse(
                r.getId(), r.getReservationId(), r.getUserId(), r.getStoreId(), r.getManagerId(),
                r.getRating(), r.getText(), r.getCreatedAt());
    }
}
