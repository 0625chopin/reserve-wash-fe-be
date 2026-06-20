package com.carwash.service;

import com.carwash.dto.BayResponse;
import com.carwash.mapper.BayMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 베이 조회 서비스 — 전체(카탈로그 캐시용)
//   차종별 노출 필터(getBaysForCar 누적 로직)는 FE에서 동기 계산(Phase 2 방침). 서버 필터는 Phase 6(M3)에서.
@Service
public class BayService {

    private final BayMapper bayMapper;

    public BayService(BayMapper bayMapper) {
        this.bayMapper = bayMapper;
    }

    @Transactional(readOnly = true)
    public List<BayResponse> findAll() {
        return bayMapper.findAll().stream().map(BayResponse::from).toList();
    }
}
