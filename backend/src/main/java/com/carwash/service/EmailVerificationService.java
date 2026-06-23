package com.carwash.service;

import com.carwash.domain.EmailVerification;
import com.carwash.domain.enums.UserRole;
import com.carwash.domain.enums.VerificationMethod;
import com.carwash.dto.LoginResponse;
import com.carwash.dto.VerificationResponse;
import com.carwash.dto.VerifyResponse;
import com.carwash.mapper.EmailVerificationMapper;
import com.carwash.mapper.UserMapper;
import java.security.SecureRandom;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// 회원가입 이메일 인증 (6자리 코드, 유효 3분) — create-after-verify.
//   요청(request): 가입 정보 검증 → 코드 생성·저장 → 코드 메일 발송(원문 비번 미보관, 해시만 저장).
//   검증(verify):  코드 일치+미만료+시도횟수 검증 → users 생성(USER 즉시 활성 / MANAGER 1차 승인 대기).
//   재전송(resend): 새 코드·만료로 갱신 후 재발송.
//   인증 성공 시에만 users 로 승격하므로 미인증 반쪽 계정이 남지 않는다(require §4.4).
@Service
public class EmailVerificationService {

    static final long TTL_MS = 180_000L;          // 코드 유효 시간 3분(= 180초, FE 3:00 카운트다운)
    static final int MAX_ATTEMPTS = 5;            // 코드 입력 시도 상한(초과 시 무효화)

    private final EmailVerificationMapper verificationMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final AuthService authService;
    private final SecureRandom random = new SecureRandom();

    public EmailVerificationService(
            EmailVerificationMapper verificationMapper,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            NotificationService notificationService,
            AuthService authService) {
        this.verificationMapper = verificationMapper;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
        this.authService = authService;
    }

    // 가입 1단계 — 코드 발급·발송. 이미 가입된 이메일이면 409. 매니저는 storeId 필수.
    @Transactional
    public VerificationResponse request(
            String email, String rawPassword, String name, UserRole role, String storeId) {
        if (userMapper.findByEmail(email) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다.");
        }
        if (role != UserRole.USER && role != UserRole.MANAGER) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "가입 가능한 역할은 USER 또는 MANAGER 입니다.");
        }
        if (role == UserRole.MANAGER && (storeId == null || storeId.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "매니저 가입은 소속 매장이 필요합니다.");
        }
        String code = generateCode();
        // email 단위 1건 — 재요청 시 기존 대기를 지우고 새로 저장(delete→insert, H2/MySQL 공통)
        verificationMapper.deleteByEmail(email);
        verificationMapper.insert(EmailVerification.builder()
                .email(email)
                .method(VerificationMethod.EMAIL.name())   // 현재 가입 인증은 EMAIL 방법
                .code(code)
                .role(role.name())
                .name(name)
                .passwordHash(passwordEncoder.encode(rawPassword))   // 원문 비번 미보관
                .storeId(storeId)
                .expiresAt(System.currentTimeMillis() + TTL_MS)
                .attempts(0)
                .build());
        notificationService.notifyEmailVerificationCode(email, name, code);
        return new VerificationResponse((int) (TTL_MS / 1000));
    }

    // 가입 2단계 — 코드 검증 후 가입 확정. USER 는 토큰 발급(자동 로그인), 매니저는 승인 대기.
    @Transactional
    public VerifyResponse verify(String email, String code) {
        EmailVerification ev = verificationMapper.findByEmail(email);
        if (ev == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "인증 요청이 없습니다. 다시 요청해 주세요.");
        }
        if (ev.isExpired(System.currentTimeMillis())) {
            verificationMapper.deleteByEmail(email);
            throw new ResponseStatusException(HttpStatus.GONE, "인증 시간이 만료되었습니다. 재전송해 주세요.");
        }
        if (ev.getAttempts() != null && ev.getAttempts() >= MAX_ATTEMPTS) {
            verificationMapper.deleteByEmail(email);
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS, "인증 시도 횟수를 초과했습니다. 다시 요청해 주세요.");
        }
        if (!ev.getCode().equals(code)) {
            verificationMapper.incrementAttempts(email);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인증 코드가 일치하지 않습니다.");
        }
        // 성공 — 대기 제거 후 가입 확정
        verificationMapper.deleteByEmail(email);
        if (UserRole.USER.name().equals(ev.getRole())) {
            LoginResponse login = authService.createVerifiedUser(
                    ev.getEmail(), ev.getPasswordHash(), ev.getName());
            return new VerifyResponse(login.token(), login.user(), false);
        }
        authService.createVerifiedManager(
                ev.getEmail(), ev.getPasswordHash(), ev.getName(),
                ev.getStoreId(), UserRole.valueOf(ev.getRole()));
        return new VerifyResponse(null, null, true);   // 매니저 — 승인 대기
    }

    // 재전송 — 새 코드·만료(3분)로 갱신하고 재발송. 대기 없으면 404.
    @Transactional
    public VerificationResponse resend(String email) {
        EmailVerification ev = verificationMapper.findByEmail(email);
        if (ev == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "인증 요청이 없습니다. 다시 요청해 주세요.");
        }
        String code = generateCode();
        verificationMapper.updateCode(email, code, System.currentTimeMillis() + TTL_MS);
        notificationService.notifyEmailVerificationCode(email, ev.getName(), code);
        return new VerificationResponse((int) (TTL_MS / 1000));
    }

    // 개발/테스트용 — 대기 중인 코드 조회(컨트롤러의 dev-code 백도어에서만 사용)
    public String peekCode(String email) {
        EmailVerification ev = verificationMapper.findByEmail(email);
        return ev == null ? null : ev.getCode();
    }

    // 6자리 숫자 코드(000000~999999, 선행 0 허용) — SecureRandom
    private String generateCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }
}
