package com.carwash.dto;

import com.carwash.domain.Bay;
import com.carwash.domain.Store;
import java.util.List;

// 관리자 매장 응답 (v2.4) — 매장 정보 + 베이 구성. FO StoreResponse와 달리 미승인 매장도 포함.
public record AdminStoreResponse(
        String id, String name, int bayCount, boolean approved, List<BayResponse> bays) {

    public static AdminStoreResponse from(Store s, List<Bay> bays) {
        return new AdminStoreResponse(
                s.getId(), s.getName(), s.getBayCount(), s.isApproved(),
                bays.stream().map(BayResponse::from).toList());
    }
}
