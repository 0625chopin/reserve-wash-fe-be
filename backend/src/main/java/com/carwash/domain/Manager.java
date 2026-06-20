package com.carwash.domain;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 매니저 (require 3.1·6.1) — 순수 POJO. FE Manager와 무변환 일치
// dayoffs는 매퍼 <collection>으로 조립(Phase 1 T5)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Manager {

    private String id;                  // 'mgr1' 등 문자열 id (무변환)
    private String storeId;
    private String name;
    private boolean isStoreAdmin;       // 매장 최고권한 매니저 여부
    @Builder.Default
    private List<ManagerDayoff> dayoffs = new ArrayList<>();   // 휴무(전일/교대조)
}
