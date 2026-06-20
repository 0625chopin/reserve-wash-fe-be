package com.carwash.controller;

import com.carwash.dto.LoginRequest;
import com.carwash.dto.LoginResponse;
import com.carwash.dto.SignupRequest;
import com.carwash.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 인증 진입점 (require 4장) — /api/auth/** 는 SecurityConfig에서 permitAll
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // 로그인 — JWT 발급
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.email(), request.password());
    }

    // 회원가입(FW1) — USER 즉시 가입 + 자동 로그인
    @PostMapping("/signup")
    public LoginResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request.email(), request.password(), request.name());
    }
}
