package com.carwash.mapper;

import com.carwash.domain.Review;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

// 후기 매퍼 — 기본 조회/삽입 + 자격검증(중복)·매니저 집계(Phase 8)
@Mapper
public interface ReviewMapper {

    Review findById(String id);

    List<Review> findByStore(String storeId);

    // 매장 삭제 무결성(v2.4) — 매장 귀속 후기 수
    int countByStore(@Param("storeId") String storeId);

    // 매니저별 후기 — 평균 평점 집계(Phase 8)
    List<Review> findByManager(String managerId);

    // 예약당 중복 작성 방지(require 9.1) — 존재 여부
    int countByReservationId(String reservationId);

    int insert(Review review);
}
