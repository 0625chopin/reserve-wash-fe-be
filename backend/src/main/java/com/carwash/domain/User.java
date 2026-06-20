package com.carwash.domain;

import com.carwash.domain.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 사용자 (require 3.1) — 순수 POJO, JPA 애너테이션 없음
// 필드명은 FE app/types/domain.ts User와 무변환 일치(id는 VARCHAR 문자열, Phase 0 id 정책)
@Getter
@NoArgsConstructor              // MyBatis 결과 매핑용 기본 생성자
@AllArgsConstructor
@Builder
public class User {

    private String id;          // 'user1' 등 문자열 id (무변환)
    private String email;
    private String name;
    private UserRole role;
    private String passwordHash; // BCrypt 해시 (Phase 3 인증) — 응답 DTO에는 절대 노출 금지
}
