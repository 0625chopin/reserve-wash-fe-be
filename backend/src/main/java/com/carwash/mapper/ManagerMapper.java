package com.carwash.mapper;

import com.carwash.domain.Manager;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

// 매니저 매퍼 — dayoffs는 <collection> 단일 조인으로 조립(N+1 회피)
@Mapper
public interface ManagerMapper {

    // 카탈로그 캐시용 전체 조회(Phase 2) — dayoffs collection 포함
    List<Manager> findAll();

    List<Manager> findByStore(String storeId);

    Manager findById(String id);
}
