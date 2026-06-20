package com.carwash.service;

import com.carwash.domain.Reservation;
import com.carwash.domain.Review;
import com.carwash.domain.enums.ReservationStatus;
import com.carwash.dto.AverageRatingResponse;
import com.carwash.dto.ReviewRequest;
import com.carwash.dto.ReviewResponse;
import com.carwash.mapper.ReservationMapper;
import com.carwash.mapper.ReviewMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// 후기/평점 서비스 (require 9장) — 작성 자격(세차완료·본인·중복) 서버 검증 + 평균 집계.
@Service
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final ReservationMapper reservationMapper;

    public ReviewService(ReviewMapper reviewMapper, ReservationMapper reservationMapper) {
        this.reviewMapper = reviewMapper;
        this.reservationMapper = reservationMapper;
    }

    // 후기 작성 — COMPLETED + 본인 예약 + 중복 방지(require 9.1). 평점 범위는 Bean Validation.
    @Transactional
    public ReviewResponse addReview(ReviewRequest req, String userId) {
        Reservation reservation = reservationMapper.findById(req.reservationId());
        if (reservation == null || !reservation.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "예약을 찾을 수 없습니다.");
        }
        if (reservation.getStatus() != ReservationStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "세차완료 예약만 후기를 작성할 수 있습니다.");
        }
        if (reviewMapper.countByReservationId(req.reservationId()) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 후기를 작성했습니다.");
        }
        Review review = Review.builder()
                .id("rv-" + UUID.randomUUID())
                .reservationId(req.reservationId())
                .userId(userId)
                .storeId(reservation.getStoreId())
                .managerId(reservation.getManagerId())
                .rating(req.rating())
                .text(req.text())
                .createdAt(Instant.now().toString())
                .build();
        reviewMapper.insert(review);
        return ReviewResponse.from(review);
    }

    // 매장 평균 평점 (require 9.1)
    @Transactional(readOnly = true)
    public AverageRatingResponse storeAverage(String storeId) {
        return average(reviewMapper.findByStore(storeId));
    }

    // 매니저 평균 평점
    @Transactional(readOnly = true)
    public AverageRatingResponse managerAverage(String managerId) {
        return average(reviewMapper.findByManager(managerId));
    }

    // 관리자 매장별 후기 확인 (S6, require 11.1)
    @Transactional(readOnly = true)
    public List<ReviewResponse> listStoreReviews(String storeId) {
        return reviewMapper.findByStore(storeId).stream().map(ReviewResponse::from).toList();
    }

    private AverageRatingResponse average(List<Review> reviews) {
        double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(0);
        return new AverageRatingResponse(avg, reviews.size());
    }
}
