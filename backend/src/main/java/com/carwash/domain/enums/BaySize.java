package com.carwash.domain.enums;

// 베이 수용 크기 등급 — 차 크기 이상을 수용하는 베이만 노출(누적 로직, Phase 0 Q3)
// Phase 0 Q1 확정(2026-06-21): 3등급 → 4등급 신설, 특대형(XLARGE) 추가. VAN_ETC→XLARGE 매핑.
// FE app/types/enums.ts BaySize도 XLARGE 동반 추가(Phase 1 T7)하여 값집합 일치.
public enum BaySize {
    SMALL,    // 소형
    MID,      // 중형
    LARGE,    // 대형
    XLARGE    // 특대형(신설) — 승합·기타(VAN_ETC) 수용
}
