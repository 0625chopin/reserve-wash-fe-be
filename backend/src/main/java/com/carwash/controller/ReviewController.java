package com.carwash.controller;

import com.carwash.dto.AverageRatingResponse;
import com.carwash.dto.ReviewRequest;
import com.carwash.dto.ReviewResponse;
import com.carwash.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 후기 작성/평균 (require 9장) — 작성은 보호 API(JWT, userId=uid). 평균 조회도 인증(로그인 사용자).
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    // 후기 작성 — 자격검증(COMPLETED·본인·중복)은 서비스가 강제. 충돌 409·미달 400·미존재 404.
    @PostMapping
    public ReviewResponse create(
            @AuthenticationPrincipal String userId, @Valid @RequestBody ReviewRequest req) {
        return reviewService.addReview(req, userId);
    }

    @GetMapping("/stores/{id}/average")
    public AverageRatingResponse storeAverage(@PathVariable String id) {
        return reviewService.storeAverage(id);
    }

    @GetMapping("/managers/{id}/average")
    public AverageRatingResponse managerAverage(@PathVariable String id) {
        return reviewService.managerAverage(id);
    }
}
