package com.carwash.service;

import com.carwash.domain.NotificationLog;
import com.carwash.domain.User;
import com.carwash.mapper.NotificationLogMapper;
import com.carwash.mapper.UserMapper;
import org.springframework.stereotype.Service;

// 알림 정책 (Phase 9, require §13.2 항목 7) — 발송 시점·수신자·본문을 결정한다.
//   호출자 트랜잭션 내에서 동기로 수신자 email·본문을 해석하고 발송 이력을 기록한 뒤, 실제 dispatch만
//   EmailSender(@Async)에 위임한다. 수신자를 동기로 해석하므로 비동기 스레드의 uncommitted-read(커밋 전
//   재조회)를 피하고, 발송 실패는 비동기라 도메인 트랜잭션(가입/예약/결재)에 전파되지 않는다.
//   정책 3종: ① 이메일 인증 링크 ② 예약 확정 안내 ③ 결재 결과 통지(매니저 가입 / 휴가·반차).
@Service
public class NotificationService {

    // 제목 접두(가독성 위해 상수 분리)
    private static final String SUBJECT_PREFIX = "[세차예약] ";
    private static final String DEFAULT_NAME = "고객";

    private final EmailSender emailSender;
    private final UserMapper userMapper;
    private final NotificationLogMapper notificationLogMapper;

    public NotificationService(
            EmailSender emailSender,
            UserMapper userMapper,
            NotificationLogMapper notificationLogMapper) {
        this.emailSender = emailSender;
        this.userMapper = userMapper;
        this.notificationLogMapper = notificationLogMapper;
    }

    // ① 이메일 인증 링크 (가입 REQUESTED, require §4.4) — User 도메인 객체로 수신자 직접 해석
    public void notifyEmailVerification(User user) {
        String to = (user == null) ? null : user.getEmail();
        String subject = SUBJECT_PREFIX + "이메일 인증을 완료해 주세요";
        String body = nameOf(user) + "님, 가입을 환영합니다.\n"
                + "아래 링크를 눌러 이메일 인증을 완료해 주세요.\n"
                + verificationLink(user);
        dispatch("EMAIL_VERIFICATION", to, subject, body);
    }

    // ② 예약 확정 안내 (예약 RESERVED) — userId로 수신자를 동기 해석(호출자 tx 컨텍스트)
    public void notifyReservationConfirmed(String userId) {
        User user = userMapper.findById(userId);
        String to = (user == null) ? null : user.getEmail();
        String subject = SUBJECT_PREFIX + "예약이 확정되었습니다";
        String body = nameOf(user) + "님, 예약이 정상적으로 확정되었습니다.";
        dispatch("RESERVATION_CONFIRMED", to, subject, body);
    }

    // ③-a 매니저 가입 승인 결과 (M7/S3 단계 전이) — User(email 보유)로 수신자 직접 해석
    public void notifySignupApprovalResult(User user, String resultLabel) {
        String to = (user == null) ? null : user.getEmail();
        String subject = SUBJECT_PREFIX + "매니저 가입 결재 결과 안내";
        String body = "매니저 가입 신청이 " + resultLabel + " 처리되었습니다.";
        dispatch("APPROVAL_RESULT", to, subject, body);
    }

    // ③-b 휴가/반차 승인 결과 (1단계) — managerId를 users.manager_id FK로 수신자 해석(미연결 시 SKIPPED)
    public void notifyDayoffApprovalResult(String managerId, String resultLabel) {
        User user = userMapper.findByManagerId(managerId);
        String to = (user == null) ? null : user.getEmail();
        String subject = SUBJECT_PREFIX + "휴가/반차 결재 결과 안내";
        String body = "휴가/반차 신청이 " + resultLabel + " 처리되었습니다.";
        dispatch("APPROVAL_RESULT", to, subject, body);
    }

    // 동기 이력 기록(호출자 tx 참여) + 수신자 있으면 비동기 발송(@Async).
    //   수신자 없음 → SKIPPED 이력만 남기고 발송하지 않음. 발송 실패는 비동기라 본 tx에 비전파.
    private void dispatch(String type, String recipient, String subject, String body) {
        boolean skipped = isBlank(recipient);
        notificationLogMapper.insert(NotificationLog.builder()
                .recipient(recipient)
                .type(type)
                .subject(subject)
                .status(skipped ? "SKIPPED" : "QUEUED")
                .build());
        if (!skipped) {
            emailSender.send(recipient, subject, body);   // @Async fire
        }
    }

    // MVP: 인증 링크는 단순 문자열(실제 토큰 발급·영속·검증은 별도 과제)
    private String verificationLink(User user) {
        return "http://localhost:3000/verify?email=" + (user == null ? "" : user.getEmail());
    }

    private String nameOf(User user) {
        return (user == null || isBlank(user.getName())) ? DEFAULT_NAME : user.getName();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
