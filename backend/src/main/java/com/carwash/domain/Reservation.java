package com.carwash.domain;

import com.carwash.domain.enums.CarType;
import com.carwash.domain.enums.ReservationStatus;
import com.carwash.domain.enums.ServiceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 예약 (require 6장·11.3) — 순수 POJO. FE Reservation과 무변환 일치
//   id는 문자열(앱이 부여, AUTO_INCREMENT 미사용 — Phase 0 id 정책)
//   managerId는 매니저 대행이 아니면 null
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    private String id;
    private String userId;
    private String storeId;
    private String bayId;
    private String managerId;       // nullable — 대행 예약이 아니면 null
    private String date;            // 'YYYY-MM-DD'
    private String timeSlot;        // 'HH:mm'
    private CarType carType;
    private ServiceType serviceType;
    private int amount;
    private ReservationStatus status;
}
