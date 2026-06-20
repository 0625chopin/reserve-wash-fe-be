package com.carwash.domain;

import com.carwash.domain.enums.ApprovalStatus;
import com.carwash.domain.enums.DayoffType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 매니저 휴무 = (날짜, 휴무 유형) (require 5.4·5.5) — Manager.dayoffs 의 원소이자 결재 워크플로우 엔티티.
//   카탈로그(Manager.dayoffs)는 date/type만 매핑하고 CONFIRMED 휴무만 노출(ManagerMapper 필터).
//   결재 워크플로우(Phase 7)는 id/managerId/status까지 다룬다(ManagerDayoffMapper).
//   FE ManagerDayoff{date,type}와 무변환 일치(추가 필드는 카탈로그 DTO에 미노출).
@Getter
@NoArgsConstructor              // MyBatis 결과 매핑용(필드 직접 접근)
public class ManagerDayoff {

    private Long id;                 // 내부 surrogate PK(BIGINT, FE 미노출)
    private String managerId;        // 휴무 대상 매니저
    private String date;             // 'YYYY-MM-DD'
    private DayoffType type;         // FULL_DAY=전일 / SHIFT_1~3=교대조 단위
    private ApprovalStatus status;   // 결재 상태(Phase 7)

    @Builder
    public ManagerDayoff(
            Long id, String managerId, String date, DayoffType type, ApprovalStatus status) {
        this.id = id;
        this.managerId = managerId;
        this.date = date;
        this.type = type;
        this.status = status;
    }

    // 결재 전이 — 불가능한 단계 건너뛰기 차단 (require 8.3). 위반 시 IllegalStateException → 409
    public void approveL1() {
        if (this.status != ApprovalStatus.SUBMITTED) {
            throw new IllegalStateException("1차 승인 불가 상태: " + this.status);
        }
        this.status = ApprovalStatus.APPROVED_L1;
    }

    public void approveL2() {
        if (this.status != ApprovalStatus.APPROVED_L1) {
            throw new IllegalStateException("2차 승인 불가 상태: " + this.status);
        }
        this.status = ApprovalStatus.CONFIRMED;   // L2 = 확정
    }

    public void reject() {
        if (this.status == ApprovalStatus.CONFIRMED || this.status == ApprovalStatus.REJECTED) {
            throw new IllegalStateException("반려 불가 상태: " + this.status);
        }
        this.status = ApprovalStatus.REJECTED;
    }

    public void resubmit() {
        if (this.status != ApprovalStatus.REJECTED) {
            throw new IllegalStateException("재신청 불가 상태: " + this.status);
        }
        this.status = ApprovalStatus.SUBMITTED;
    }
}
