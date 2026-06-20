package com.carwash.mapper;

import com.carwash.domain.Reservation;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

// 예약 매퍼 — Phase 1은 기본 조회/삽입만(상태 전이 API는 Phase 4·5)
@Mapper
public interface ReservationMapper {

    Reservation findById(String id);

    List<Reservation> findByUser(String userId);

    int insert(Reservation reservation);
}
