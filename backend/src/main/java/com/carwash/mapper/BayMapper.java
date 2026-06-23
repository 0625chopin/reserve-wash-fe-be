package com.carwash.mapper;

import com.carwash.domain.Bay;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

// 베이 매퍼 — SQL은 resources/mapper/BayMapper.xml
@Mapper
public interface BayMapper {

    // 카탈로그 캐시용 전체 조회(Phase 2)
    List<Bay> findAll();

    List<Bay> findByStore(String storeId);

    Bay findById(String id);

    // 관리자 매장 CRUD (v2.4) — 매장 생성/수정 시 베이 구성 반영
    int insert(Bay bay);

    int deleteByStore(@Param("storeId") String storeId);

    int countByStore(@Param("storeId") String storeId);
}
