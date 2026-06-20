package com.carwash.domain;

import com.carwash.domain.enums.DayoffApprovalStatus;
import com.carwash.domain.enums.DayoffType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 매니저 휴무 = (날짜, 휴무 유형) (require 5.4·5.5) — Manager.dayoffs 의 원소이자 결재 워크플로우 엔티티.
//   카탈로그(Manager.dayoffs)는 date/type만 매핑하고 APPROVED 휴무만 노출(ManagerMapper 필터).
//   결재 워크플로우(M6 신청·M8 승인)는 id/managerId/status까지 다룬다(ManagerDayoffMapper).
//   FE ManagerDayoff{date,type}와 무변환 일치(추가 필드는 카탈로그 DTO에 미노출).
@Getter
@NoArgsConstructor              // MyBatis 결과 매핑용(필드 직접 접근)
public class ManagerDayoff {

    private Long id;                       // 내부 surrogate PK(BIGINT, FE 미노출)
    private String managerId;              // 휴무 대상 매니저
    private String date;                   // 'YYYY-MM-DD'
    private DayoffType type;               // FULL_DAY=전일 / SHIFT_1~3=교대조 단위
    private DayoffApprovalStatus status;   // 휴가/반차 1단계 결재 상태 (require v1.7 §8.3)

    @Builder
    public ManagerDayoff(
            Long id, String managerId, String date, DayoffType type, DayoffApprovalStatus status) {
        this.id = id;
        this.managerId = managerId;
        this.date = date;
        this.type = type;
        this.status = status;
    }

    // 휴가/반차 1단계 승인 — 매장매니저관리자(STORE_ADMIN)가 SUBMITTED → APPROVED로 종결(M8).
    //   관리자 개입 없음(require v1.7 §8.2). 불가 전이 시 IllegalStateException → 409.
    public void approve() {
        if (this.status != DayoffApprovalStatus.SUBMITTED) {
            throw new IllegalStateException("승인 불가 상태: " + this.status);
        }
        this.status = DayoffApprovalStatus.APPROVED;   // 1단계 종결 = 확정
    }

    public void reject() {
        if (this.status != DayoffApprovalStatus.SUBMITTED) {
            throw new IllegalStateException("반려 불가 상태: " + this.status);
        }
        this.status = DayoffApprovalStatus.REJECTED;
    }

    public void resubmit() {
        if (this.status != DayoffApprovalStatus.REJECTED) {
            throw new IllegalStateException("재신청 불가 상태: " + this.status);
        }
        this.status = DayoffApprovalStatus.SUBMITTED;
    }
}
