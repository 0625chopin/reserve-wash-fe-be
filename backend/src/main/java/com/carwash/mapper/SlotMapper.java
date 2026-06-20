package com.carwash.mapper;

import com.carwash.domain.Slot;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

// 슬롯 매퍼 — (매장,베이,날짜,시간) 키 조회 + 점유(INSERT)/낙관·비관 락(Phase 4)
@Mapper
public interface SlotMapper {

    // (매장, 베이, 날짜, 시간)으로 슬롯 단건 조회 — 점유/확정 시 사용
    Slot findByKey(@Param("storeId") String storeId,
                   @Param("bayId") String bayId,
                   @Param("date") String date,
                   @Param("timeSlot") String timeSlot);

    // 희소 슬롯 점유 — INSERT HOLDING. 동시 INSERT 시 uk_slot UNIQUE 위반으로 1건만 성공(최종 방어선, require 7.3)
    int insertHold(@Param("storeId") String storeId,
                   @Param("bayId") String bayId,
                   @Param("date") String date,
                   @Param("timeSlot") String timeSlot);

    // 비관적 락 — SELECT ... FOR UPDATE(경합 잦은 인기 슬롯 시연 경로, require 7.3)
    Slot findForUpdate(@Param("storeId") String storeId,
                       @Param("bayId") String bayId,
                       @Param("date") String date,
                       @Param("timeSlot") String timeSlot);

    // 매장·날짜 단위 점유 슬롯 배치 조회 — FE 그리드 하이드레이트(GET /api/slots)
    List<Slot> findByStoreAndDate(@Param("storeId") String storeId, @Param("date") String date);

    // 낙관적 락: version 일치 시에만 갱신, 영향 행 수 0이면 충돌(require 7.3)
    int updateStatusWithVersion(@Param("id") Long id,
                                @Param("status") String status,
                                @Param("version") Long version);
}
