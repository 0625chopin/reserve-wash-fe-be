package com.carwash.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

// SMTP 메일 발송 구현 (Phase 9) — JavaMailSender 기반.
//   @Async("mailTaskExecutor")로 발송 전용 풀에서 비동기 실행 → 본 트랜잭션(가입/예약/결재)을
//   차단하지 않고, 발송 실패도 별도 스레드라 도메인 흐름에 전파(롤백)되지 않는다.
//   NotificationService와 별도 빈이므로 self-invocation 무효화 문제 없음(프록시 경유 정상 동작).
@Component
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;

    public SmtpEmailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async("mailTaskExecutor")
    @Override
    public void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);   // 발송 실패 시 예외는 이 비동기 스레드에 격리됨
    }
}
