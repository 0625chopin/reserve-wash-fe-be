package com.carwash.dto;

import com.carwash.domain.enums.BaySize;
import java.util.List;

// 관리자 매장 등록/수정 요청 (v2.4) — 베이 구성·승인 상태 포함.
//   approved는 nullable: 미입력(null) 시 false(미승인)로 생성한다(🔒 정책 확정 — 등록 직후 미승인).
public record AdminStoreRequest(String name, int bayCount, Boolean approved, List<BayInput> bays) {

    // 베이 입력 — 'A1'~'AN' 코드 + 수용 크기 등급
    public record BayInput(String code, BaySize size) {}

    // 미입력(null)이면 미승인(false) — POST 기본값 정책
    public boolean approvedOrDefault() {
        return approved != null && approved;
    }

    // null 안전 베이 목록
    public List<BayInput> baysOrEmpty() {
        return bays != null ? bays : List.of();
    }
}
