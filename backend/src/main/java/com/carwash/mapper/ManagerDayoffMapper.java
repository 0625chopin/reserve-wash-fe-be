package com.carwash.mapper;

import com.carwash.domain.ManagerDayoff;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

// 매니저 휴무 결재 워크플로우 매퍼 (Phase 7) — 신청/조회/상태전이.
//   카탈로그(Manager.dayoffs)와 동일 manager_dayoff 테이블을 쓰되, 여기선 id/status 포함 전체를 다룬다.
@Mapper
public interface ManagerDayoffMapper {

    int insert(ManagerDayoff dayoff);          // useGeneratedKeys → id 채움

    ManagerDayoff findById(Long id);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    List<ManagerDayoff> findByStore(String storeId);   // 매장 매니저들의 휴무 신청(L1 검토용)

    List<ManagerDayoff> findAll();                      // 전체(관리자 L2 결재함)
}
