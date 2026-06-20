package com.carwash.domain.enums;

// 서비스 4분류 (require 10.2 확정) — FE ServiceType 리터럴과 글자까지 일치
public enum ServiceType {
    EXT,      // 외부세차
    INT,      // 내부세차
    FULL,     // 풀패키지(외부+내부)
    PREMIUM   // 프리미엄(왁스·광택)
}
