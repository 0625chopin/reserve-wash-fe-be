package com.carwash.mapper;

import com.carwash.domain.Bay;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

// 베이 매퍼 — SQL은 resources/mapper/BayMapper.xml
@Mapper
public interface BayMapper {

    // 카탈로그 캐시용 전체 조회(Phase 2)
    List<Bay> findAll();

    List<Bay> findByStore(String storeId);

    Bay findById(String id);
}
