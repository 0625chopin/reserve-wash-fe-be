package com.carwash.domain;

import com.carwash.domain.enums.ApprovalStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 매장 휴일 (require 5.4·8.1) — 매니저 신청 → 관리자 1단계 승인(Phase 7 결재)
//   id는 DB 내부 surrogate(BIGINT, FE 미노출). 결재 상태머신을 status로 표현.
@Getter
@NoArgsConstructor              // MyBatis 결과 매핑용(필드 직접 접근)
public class StoreHoliday {

    private Long id;                 // 내부 surrogate PK
    private String storeId;
    private String date;             // 'YYYY-MM-DD'
    private ApprovalStatus status;   // 결재 상태(SUBMITTED→CONFIRMED / REJECTED)

    @Builder
    public StoreHoliday(Long id, String storeId, String date, ApprovalStatus status) {
        this.id = id;
        this.storeId = storeId;
        this.date = date;
        this.status = status;
    }

    // 단일 승인(관리자) — SUBMITTED → CONFIRMED (require 8.1). 위반 시 IllegalStateException → 409
    public void approve() {
        if (this.status != ApprovalStatus.SUBMITTED) {
            throw new IllegalStateException("승인 불가 상태: " + this.status);
        }
        this.status = ApprovalStatus.CONFIRMED;
    }

    public void reject() {
        if (this.status == ApprovalStatus.CONFIRMED || this.status == ApprovalStatus.REJECTED) {
            throw new IllegalStateException("반려 불가 상태: " + this.status);
        }
        this.status = ApprovalStatus.REJECTED;
    }
}
