package com.carwash.dto;

import com.carwash.domain.Store;

// 매장 응답 DTO — FE app/types/domain.ts Store와 필드명 무변환 일치
public record StoreResponse(String id, String name, int bayCount, boolean approved) {

    public static StoreResponse from(Store s) {
        return new StoreResponse(s.getId(), s.getName(), s.getBayCount(), s.isApproved());
    }
}
