package com.carwash.controller;

import com.carwash.dto.LoginRequest;
import com.carwash.dto.LoginResponse;
import com.carwash.dto.ResendCodeRequest;
import com.carwash.dto.SignupCodeRequest;
import com.carwash.dto.SignupManagerRequest;
import com.carwash.dto.SignupRequest;
import com.carwash.dto.VerificationResponse;
import com.carwash.dto.VerifyCodeRequest;
import com.carwash.dto.VerifyResponse;
import com.carwash.service.AuthService;
import com.carwash.service.EmailVerificationService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// 인증 진입점 (require 4장) — /api/auth/** 는 SecurityConfig에서 permitAll
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    // 개발/테스트 전용 — 발급된 인증 코드 조회 백도어 노출 여부(운영 프로파일에서 false)
    private final boolean devCodePeek;

    public AuthController(
            AuthService authService,
            EmailVerificationService emailVerificationService,
            @Value("${app.signup.dev-code-peek:false}") boolean devCodePeek) {
        this.authService = authService;
        this.emailVerificationService = emailVerificationService;
        this.devCodePeek = devCodePeek;
    }

    // 로그인 — JWT 발급
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.email(), request.password());
    }

    // ── 이메일 인증 가입 플로우 (6자리 코드, 유효 3분) ─────────────────────

    // 가입 1단계 — 코드 발송. 응답의 expiresInSec 로 FE 가 3:00 카운트다운 시작.
    @PostMapping("/signup/request")
    public VerificationResponse requestSignupCode(@Valid @RequestBody SignupCodeRequest request) {
        return emailVerificationService.request(
                request.email(), request.password(), request.name(), request.role(), request.storeId());
    }

    // 가입 2단계 — 코드 검증·가입 확정. USER 는 token+user(자동 로그인), 매니저는 pendingApproval=true.
    @PostMapping("/signup/verify")
    public VerifyResponse verifySignupCode(@Valid @RequestBody VerifyCodeRequest request) {
        return emailVerificationService.verify(request.email(), request.code());
    }

    // 코드 재전송 — 새 코드·만료(3분) 갱신
    @PostMapping("/signup/resend")
    public VerificationResponse resendSignupCode(@Valid @RequestBody ResendCodeRequest request) {
        return emailVerificationService.resend(request.email());
    }

    // 개발/테스트 전용 — 대기 중인 인증 코드 조회(운영에선 app.signup.dev-code-peek=false → 404)
    @GetMapping("/signup/dev-code")
    public Map<String, String> devCode(@RequestParam String email) {
        if (!devCodePeek) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "not found");
        }
        String code = emailVerificationService.peekCode(email);
        if (code == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "인증 요청이 없습니다.");
        }
        return Map.of("email", email, "code", code);
    }

    // ── 레거시 직접 가입(이메일 인증 미적용) — 내부/호환용. FE 는 위 인증 플로우를 사용한다. ──

    // 회원가입(FW1) — USER 즉시 가입 + 자동 로그인
    @PostMapping("/signup")
    public LoginResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request.email(), request.password(), request.name());
    }

    // 매니저 회원가입(M1, require v1.9) — 소속 매장 지정, PENDING_APPROVAL_L1로 신청(자동 로그인 없음)
    @PostMapping("/signup-manager")
    @ResponseStatus(HttpStatus.CREATED)
    public void signupManager(@Valid @RequestBody SignupManagerRequest request) {
        authService.signupManager(
                request.email(), request.password(), request.name(), request.storeId());
    }
}
