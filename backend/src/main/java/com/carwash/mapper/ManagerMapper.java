package com.carwash.mapper;

import com.carwash.domain.Manager;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

// 매니저 매퍼 — dayoffs는 <collection> 단일 조인으로 조립(N+1 회피)
@Mapper
public interface ManagerMapper {

    // 카탈로그 캐시용 전체 조회(Phase 2) — dayoffs collection 포함
    List<Manager> findAll();

    List<Manager> findByStore(String storeId);

    Manager findById(String id);

    // 매장 삭제 무결성(v2.4) — 매장 귀속 매니저 수
    int countByStore(@Param("storeId") String storeId);

    // 매니저 가입 최종 승인 시 manager 엔티티 생성(v2.4) — 예약 배정 대상으로 노출되게 함
    int insert(Manager manager);
}
