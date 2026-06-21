package com.carwash.mapper;

import com.carwash.domain.EmailVerification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

// 이메일 인증 대기 매퍼 — email 단위 1건(재요청·재전송 시 delete→insert 또는 갱신).
@Mapper
public interface EmailVerificationMapper {

    EmailVerification findByEmail(String email);

    int insert(EmailVerification verification);

    int deleteByEmail(String email);

    // 코드 입력 시도 횟수 +1 (불일치 시)
    int incrementAttempts(String email);

    // 재전송 — 새 코드·만료로 갱신하고 시도 횟수 초기화
    int updateCode(
            @Param("email") String email,
            @Param("code") String code,
            @Param("expiresAt") long expiresAt);
}
