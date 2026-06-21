package com.carwash.service;

// 메일 전송 추상화 (Phase 9) — 실제 전송 메커니즘만 책임진다.
//   정책·수신자 해석·이력은 NotificationService가 담당하고, 본 인터페이스는 "보내기"에 한정한다.
//   구현(SmtpEmailSender)이 @Async라 호출자 트랜잭션과 분리되어 발송 실패가 도메인 흐름에 전파되지 않는다.
public interface EmailSender {

    // 단순 텍스트 메일 발송 (수신자·제목·본문은 호출 측에서 이미 해석된 값)
    void send(String to, String subject, String body);
}
