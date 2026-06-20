package com.carwash.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 매장 (require 5.1) — 순수 POJO. FE Store와 무변환 일치
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Store {

    private String id;          // 'store1' 등 문자열 id (무변환)
    private String name;
    private int bayCount;       // 동일 시간대 최대 수용 = 베이 수 N (require 5.2)
    private boolean approved;   // 승인된 매장만 노출 (require 6.1)
}
