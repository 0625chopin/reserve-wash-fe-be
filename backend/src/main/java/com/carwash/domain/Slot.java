package com.carwash.domain;

import com.carwash.domain.enums.SlotStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 슬롯 = (매장, 베이, 날짜, 30분 시간단위) (require 5.2)
// UNIQUE 제약은 schema.sql DDL이 담당(동시성 최종 방어선, require 7.3)
// FE Slot{storeId,bayId,date,timeSlot,status}와 무변환 일치.
//   id·version은 DB 내부 surrogate(FE 미노출) — id 정책(Phase 0): Slot만 BIGINT 내부 PK 허용.
@Getter
@NoArgsConstructor              // MyBatis 결과 매핑용 기본 생성자
public class Slot {

    private Long id;             // 내부 surrogate PK (BIGINT AUTO_INCREMENT, FE 미노출)
    private String storeId;     // 컬럼 store_id ↔ camelCase 자동 매핑
    private String bayId;
    private String date;        // 'YYYY-MM-DD'
    private String timeSlot;    // 'HH:mm' (30분 단위 시작 시각)
    private SlotStatus status;  // AVAILABLE / HOLDING / RESERVED / COMPLETED
    private Long version;       // 낙관적 락용 버전 (Phase 4에서 활용, require 7.3)

    @Builder
    public Slot(String storeId, String bayId, String date, String timeSlot, SlotStatus status) {
        this.storeId = storeId;
        this.bayId = bayId;
        this.date = date;
        this.timeSlot = timeSlot;
        this.status = status;
    }

    // 상태 전이는 도메인 메서드로 표현(setter 미개방)
    public void hold() {
        if (this.status != SlotStatus.AVAILABLE) {
            throw new IllegalStateException("점유 불가 상태: " + this.status);
        }
        this.status = SlotStatus.HOLDING;
    }

    public void reserve() {
        this.status = SlotStatus.RESERVED;
    }

    public void release() {
        this.status = SlotStatus.AVAILABLE;
    }

    // 세차완료 — 슬롯을 COMPLETED로 고정(예약 전이의 종속, 호출부가 RESERVED 보장, require 11.3)
    public void complete() {
        this.status = SlotStatus.COMPLETED;
    }
}
