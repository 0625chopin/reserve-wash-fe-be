package com.carwash.controller;

import com.carwash.dto.ReviewResponse;
import com.carwash.service.ReviewService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 관리자 매장별 후기 확인 (S6, require 11.1) — /api/admin/** 는 SecurityConfig 경로 인가로 ADMIN 한정.
@RestController
@RequestMapping("/api/admin")
public class AdminReviewController {

    private final ReviewService reviewService;

    public AdminReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/stores/{id}/reviews")
    public List<ReviewResponse> reviews(@PathVariable String id) {
        return reviewService.listStoreReviews(id);
    }
}
