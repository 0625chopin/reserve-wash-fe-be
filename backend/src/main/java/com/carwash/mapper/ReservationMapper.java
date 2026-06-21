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

    // 담당 매니저별 예약 조회 — 매니저 담당 목록(require v1.10 §6.6). 일반 예약(사용자가 지정)+대행 예약 모두 manager_id로 귀속.
    List<Reservation> findByManager(String managerId);

    // 매장별 예약 조회 — BO 관리자 예약자/사용자 관리(S4·S5, Phase 6) + 매장 전체 예약 목록(require v1.10 §6.6 STORE_ADMIN)
    List<Reservation> findByStore(String storeId);

    int insert(Reservation reservation);

    // 상태 전이 영속화 — id로 status만 갱신(전이 가드는 도메인 메서드가 담당)
    int updateStatus(@Param("id") String id, @Param("status") String status);
}
