package com.carwash.service;

import com.carwash.domain.User;
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

// 인증 서비스 (require 4장) — 로그인(JWT 발급)·회원가입(USER 즉시 가입)
// 이메일 인증/승인 상태머신은 이연(Phase 6/7/9).
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
    @Transactional(readOnly = true)
    public LoginResponse login(String email, String rawPassword) {
        User user = userMapper.findByEmail(email);
        if (user == null || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
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
                .build();
        userMapper.insert(user);
        return new LoginResponse(tokenProvider.createToken(user), UserResponse.from(user));
    }
}
