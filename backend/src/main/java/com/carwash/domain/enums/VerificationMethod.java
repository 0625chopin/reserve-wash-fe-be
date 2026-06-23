package com.carwash.domain.enums;

// 인증 방법 (인증 방법 확장 대비) — verification.method 컬럼에 name() 문자열로 저장.
//   EMAIL: 6자리 코드 메일 발송형(현재 기본). SNS: 소셜 로그인 연동(확장 예정).
public enum VerificationMethod {
    EMAIL,
    SNS
}
