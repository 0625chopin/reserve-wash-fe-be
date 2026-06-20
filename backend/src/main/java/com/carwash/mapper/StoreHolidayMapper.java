package com.carwash.mapper;

import com.carwash.domain.StoreHoliday;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

// 매장 휴일 결재 매퍼 (Phase 7) — 신청/조회/상태전이(단일 승인)
@Mapper
public interface StoreHolidayMapper {

    int insert(StoreHoliday holiday);          // useGeneratedKeys → id 채움

    StoreHoliday findById(Long id);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    List<StoreHoliday> findByStore(String storeId);

    List<StoreHoliday> findAll();
}
