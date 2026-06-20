package com.carwash.dto;

import com.carwash.domain.Manager;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

// 매니저 응답 DTO — FE Manager와 무변환 일치(dayoffs 중첩 포함)
//   isStoreAdmin: record 컴포넌트의 boolean 직렬화 키가 'storeAdmin'으로 변형되는 것을 막기 위해
//   @JsonProperty로 JSON 키를 'isStoreAdmin'으로 고정(FE Manager.isStoreAdmin과 일치)
public record ManagerResponse(
        String id,
        String storeId,
        String name,
        @JsonProperty("isStoreAdmin") boolean isStoreAdmin,
        List<DayoffResponse> dayoffs) {

    public static ManagerResponse from(Manager m) {
        List<DayoffResponse> dayoffs = m.getDayoffs().stream().map(DayoffResponse::from).toList();
        return new ManagerResponse(m.getId(), m.getStoreId(), m.getName(), m.isStoreAdmin(), dayoffs);
    }
}
