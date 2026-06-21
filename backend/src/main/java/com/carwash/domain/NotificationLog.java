package com.carwash.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 알림 발송 이력 (Phase 9) — 발송 추적성. 수신자 해석 시점에 호출자 트랜잭션에서 동기 기록한다.
//   실제 메일 dispatch는 비동기(@Async)라, 본 이력은 '발송 시도(QUEUED)/수신자 없음(SKIPPED)' 기준으로 남긴다.
//   id는 내부 surrogate(BIGINT AUTO_INCREMENT), created_at은 DB DEFAULT로 기록(조회 전용).
@Getter
@NoArgsConstructor              // MyBatis 결과 매핑용
public class NotificationLog {

    private Long id;            // 내부 surrogate PK
    private String recipient;  // 수신자 email (스킵 시 NULL 가능)
    private String type;       // EMAIL_VERIFICATION / RESERVATION_CONFIRMED / APPROVAL_RESULT
    private String subject;    // 발송 제목
    private String status;     // QUEUED(발송 시도) / SKIPPED(수신자 없음)
    private String createdAt;  // DB DEFAULT CURRENT_TIMESTAMP (insert 시 미설정)

    @Builder
    public NotificationLog(
            Long id, String recipient, String type, String subject, String status, String createdAt) {
        this.id = id;
        this.recipient = recipient;
        this.type = type;
        this.subject = subject;
        this.status = status;
        this.createdAt = createdAt;
    }
}
