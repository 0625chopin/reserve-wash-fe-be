package com.carwash.mapper;

import com.carwash.domain.Reservation;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

// 예약 매퍼 — 기본 조회/삽입 + 상태 전이 UPDATE(Phase 5 세차완료/취소)
@Mapper
public interface ReservationMapper {

    Reservation findById(String id);

    List<Reservation> findByUser(String userId);

    int insert(Reservation reservation);

    // 상태 전이 영속화 — id로 status만 갱신(전이 가드는 도메인 메서드가 담당)
    int updateStatus(@Param("id") String id, @Param("status") String status);
}
