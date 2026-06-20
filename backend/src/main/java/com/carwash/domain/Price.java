package com.carwash.domain;

import com.carwash.domain.enums.CarType;
import com.carwash.domain.enums.ServiceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 가격 — 차종 × 서비스 단가 (require 10.3) — 순수 POJO. FE Price와 무변환 일치
// DB는 (car_type, service_type) 복합 PK(Phase 0 id 정책 — 별도 id 없음)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Price {

    private CarType carType;
    private ServiceType serviceType;
    private int amount;         // 원 단위
}
