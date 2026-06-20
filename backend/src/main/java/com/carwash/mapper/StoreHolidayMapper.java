package com.carwash.mapper;

import com.carwash.domain.StoreHoliday;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

// 매장 휴일 매퍼 — Phase 1은 기본 조회/삽입만(결재 워크플로우는 Phase 7)
@Mapper
public interface StoreHolidayMapper {

    List<StoreHoliday> findByStore(String storeId);

    int insert(StoreHoliday holiday);
}
