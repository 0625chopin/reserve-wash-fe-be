package com.carwash.service;

import com.carwash.domain.enums.ReservationStatus;
import com.carwash.dto.SalesByStoreResponse;
import com.carwash.dto.SalesResponse;
import com.carwash.mapper.ReservationMapper;
import com.carwash.mapper.StoreMapper;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 매출 집계 서비스 (S8, require 11.1) — 매장별 COMPLETED 예약 금액 합산.
//   기존 ReservationMapper.findByStore(Phase 6)를 재사용해 별도 집계 SQL 없이 Java로 합산.
@Service
@Transactional(readOnly = true)
public class SalesService {

    private final ReservationMapper reservationMapper;
    private final StoreMapper storeMapper;

    public SalesService(ReservationMapper reservationMapper, StoreMapper storeMapper) {
        this.reservationMapper = reservationMapper;
        this.storeMapper = storeMapper;
    }

    public SalesResponse storeSales(String storeId) {
        return new SalesResponse(storeId, completedTotal(storeId));
    }

    // 전 매장 매출 비중 집계(v2.4) — 매장별 COMPLETED 금액 합산, 금액 내림차순.
    //   상위 5개 + ETC 합산·비중(%)은 FE(buildSalesSlices)에서 가공한다.
    public List<SalesByStoreResponse> salesByStore() {
        return storeMapper.findAll().stream()
                .map(s -> new SalesByStoreResponse(s.getId(), s.getName(), completedTotal(s.getId())))
                .sorted(Comparator.comparingLong(SalesByStoreResponse::amount).reversed())
                .toList();
    }

    private long completedTotal(String storeId) {
        return reservationMapper.findByStore(storeId).stream()
                .filter(r -> r.getStatus() == ReservationStatus.COMPLETED)
                .mapToLong(r -> r.getAmount())
                .sum();
    }
}
