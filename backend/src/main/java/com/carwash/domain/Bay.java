package com.carwash.domain;

import com.carwash.domain.enums.BaySize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 베이 — 매장 내 개별 세차 라인 (require 5.1) — 순수 POJO. FE Bay와 무변환 일치
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bay {

    private String id;          // 'store1-A1' 등 문자열 id (무변환)
    private String storeId;     // 컬럼 store_id ↔ camelCase 자동 매핑
    private String code;        // 'A1' ~ 'AN' (매장 내 식별자, 등급과 분리 — Phase 0 Q4)
    private BaySize size;       // 수용 가능한 차 크기 등급(이 등급 이하 차 수용, 누적 로직)
}
