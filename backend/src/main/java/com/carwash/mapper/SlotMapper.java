package com.carwash.mapper;

import com.carwash.domain.Slot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

// 슬롯 매퍼 — (매장,베이,날짜,시간) 키 조회 + 낙관적 락 갱신(Phase 4 선반영)
@Mapper
public interface SlotMapper {

    // (매장, 베이, 날짜, 시간)으로 슬롯 단건 조회 — 점유/확정 시 사용
    Slot findByKey(@Param("storeId") String storeId,
                   @Param("bayId") String bayId,
                   @Param("date") String date,
                   @Param("timeSlot") String timeSlot);

    // 낙관적 락: version 일치 시에만 갱신, 영향 행 수 0이면 충돌(Phase 4, require 7.3)
    int updateStatusWithVersion(@Param("id") Long id,
                                @Param("status") String status,
                                @Param("version") Long version);
}
