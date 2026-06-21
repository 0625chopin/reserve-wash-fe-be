package com.carwash.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 이메일 인증 대기 가입 (create-after-verify) — 가입 정보 + 6자리 코드 + 만료(3분)를 보관한다.
//   인증 성공 시에만 이 정보로 users 를 생성하므로, 미인증 반쪽 계정이 남지 않는다.
//   role/storeId 로 인증 후 생성 경로(USER 즉시 활성 / MANAGER 1차 승인 대기)를 결정한다.
@Getter
@NoArgsConstructor              // MyBatis 결과 매핑용
public class EmailVerification {

    private String email;          // 인증 대상(가입 예정 이메일)
    private String code;           // 6자리 숫자 코드
    private String role;           // 가입 역할(UserRole name): USER / MANAGER
    private String name;
    private String passwordHash;   // BCrypt 해시(원문 미보관)
    private String storeId;        // 매니저 가입 소속 매장(USER 는 NULL)
    private Long expiresAt;        // 만료 시각(epoch millis)
    private Integer attempts;      // 코드 입력 시도 횟수

    @Builder
    public EmailVerification(
            String email, String code, String role, String name,
            String passwordHash, String storeId, Long expiresAt, Integer attempts) {
        this.email = email;
        this.code = code;
        this.role = role;
        this.name = name;
        this.passwordHash = passwordHash;
        this.storeId = storeId;
        this.expiresAt = expiresAt;
        this.attempts = attempts;
    }

    // 만료 여부(주어진 현재 시각 기준)
    public boolean isExpired(long nowMillis) {
        return expiresAt == null || nowMillis > expiresAt;
    }
}
