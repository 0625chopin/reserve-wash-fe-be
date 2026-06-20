package com.carwash.dto;

import com.carwash.domain.Bay;
import com.carwash.domain.enums.BaySize;

// 베이 응답 DTO — FE Bay와 무변환 일치. size enum은 Jackson 기본 name 직렬화(SMALL/MID/LARGE/XLARGE)
public record BayResponse(String id, String storeId, String code, BaySize size) {

    public static BayResponse from(Bay b) {
        return new BayResponse(b.getId(), b.getStoreId(), b.getCode(), b.getSize());
    }
}
