package com.carwash.service;

import com.carwash.domain.enums.ReservationStatus;
import com.carwash.dto.SalesResponse;
import com.carwash.mapper.ReservationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 매출 집계 서비스 (S8, require 11.1) — 매장별 COMPLETED 예약 금액 합산.
//   기존 ReservationMapper.findByStore(Phase 6)를 재사용해 별도 집계 SQL 없이 Java로 합산.
@Service
@Transactional(readOnly = true)
public class SalesService {

    private final ReservationMapper reservationMapper;

    public SalesService(ReservationMapper reservationMapper) {
        this.reservationMapper = reservationMapper;
    }

    public SalesResponse storeSales(String storeId) {
        long total = reservationMapper.findByStore(storeId).stream()
                .filter(r -> r.getStatus() == ReservationStatus.COMPLETED)
                .mapToLong(r -> r.getAmount())
                .sum();
        return new SalesResponse(storeId, total);
    }
}
