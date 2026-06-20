package com.carwash.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 매장 휴일 (require 5.4) — 매니저 신청 → 관리자 승인 (Phase 7 결재 연계)
// FE domain.ts에는 타입 없음(무변환 대상 아님). require 5.4 표 기준 속성만 정의.
//   id는 DB 내부 surrogate(BIGINT, FE 미노출)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreHoliday {

    private Long id;            // 내부 surrogate PK
    private String storeId;
    private String date;       // 'YYYY-MM-DD'
    private boolean approved;  // 관리자 승인 여부
}
