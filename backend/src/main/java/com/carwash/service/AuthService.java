package com.carwash.service;

import com.carwash.domain.User;
import com.carwash.domain.enums.UserApprovalStatus;
import com.carwash.domain.enums.UserRole;
import com.carwash.dto.LoginResponse;
import com.carwash.dto.UserResponse;
import com.carwash.mapper.UserMapper;
import com.carwash.security.JwtTokenProvider;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// 인증 서비스 (require v1.7 §4장) — 로그인(JWT 발급)·회원가입(USER 즉시 가입)
//   로그인은 ACTIVE 상태만 허용(매니저 계열 2단계 승인 통과 전 차단, require §4.4).
//   이메일 인증/SMTP는 이연. 매니저 가입 2단계 승인은 SignupApprovalService(M7→S3).
@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(
            UserMapper userMapper, PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    // 이메일/비번 검증 후 JWT 발급 (실패 시 401 — 계정/비번 구분 없는 통합 응답)
    //   비번 일치해도 ACTIVE가 아니면 로그인 거부(403) — 매니저 계열 가입 2단계 승인 전 차단(require v1.7 §4.4)
    @Transactional(readOnly = true)
    public LoginResponse login(String email, String rawPassword) {
        User user = userMapper.findByEmail(email);
        if (user == null || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        if (user.getApprovalStatus() != UserApprovalStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "아직 승인 대기 중인 계정입니다.");
        }
        return new LoginResponse(tokenProvider.createToken(user), UserResponse.from(user));
    }

    // 일반 사용자(USER) 즉시 가입 — 이메일 중복 시 409, 성공 시 자동 로그인(토큰 발급)
    @Transactional
    public LoginResponse signup(String email, String rawPassword, String name) {
        if (userMapper.findByEmail(email) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다.");
        }
        User user = User.builder()
                .id("user-" + UUID.randomUUID())   // 시드 id(user1..)와 충돌 회피
                .email(email)
                .name(name)
                .role(UserRole.USER)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .approvalStatus(UserApprovalStatus.ACTIVE)   // USER는 가입 즉시 활성(승인 분기 없음, require §4.4)
                .build();
        userMapper.insert(user);
        return new LoginResponse(tokenProvider.createToken(user), UserResponse.from(user));
    }
}
