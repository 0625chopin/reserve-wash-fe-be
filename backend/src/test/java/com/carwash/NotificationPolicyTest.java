package com.carwash;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.carwash.domain.enums.CarType;
import com.carwash.domain.enums.DayoffType;
import com.carwash.domain.enums.ServiceType;
import com.carwash.dto.ConfirmRequest;
import com.carwash.dto.DayoffApprovalResponse;
import com.carwash.dto.DayoffRequest;
import com.carwash.service.ApprovalService;
import com.carwash.service.AuthService;
import com.carwash.service.EmailSender;
import com.carwash.service.ReservationService;
import com.carwash.service.SignupApprovalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

// Phase 9 알림 정책 검증 — EmailSender를 MockBean으로 대체(SmtpEmailSender의 @Async 제거 → 동기 검증).
//   NotificationService가 수신자를 해석해 정책 3종(인증/예약확정/결재결과)을 발송하고 이력을 남기는지,
//   미연결 매니저는 발송하지 않고 SKIPPED 이력만 남기는지 결정적으로 단정한다(외부 SMTP 의존 0).
//   @MockBean은 테스트 메서드마다 reset되어 verify 카운트가 격리된다.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class NotificationPolicyTest {

    @MockBean
    private EmailSender emailSender;

    @Autowired private AuthService authService;
    @Autowired private ReservationService reservationService;
    @Autowired private ApprovalService approvalService;
    @Autowired private SignupApprovalService signupApprovalService;
    @Autowired private JdbcTemplate jdbc;

    private int logCount(String type, String status) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM notification_log WHERE type = ? AND status = ?",
                Integer.class, type, status);
    }

    @Test
    void 가입_시_이메일_인증_메일과_이력() {
        authService.signup("newuser@test.com", "password", "신규고객");
        verify(emailSender).send(eq("newuser@test.com"), contains("이메일 인증"), anyString());
        assertThat(logCount("EMAIL_VERIFICATION", "QUEUED")).isGreaterThanOrEqualTo(1);
    }

    @Test
    void 예약_확정_시_확정_안내_메일과_이력() {
        ConfirmRequest req = new ConfirmRequest(
                "store2", "store2-A1", "2026-07-01", "09:00",
                null, CarType.SMALL, ServiceType.EXT, 12000);
        reservationService.confirm("user1", req);   // user1 = user@test.com
        verify(emailSender).send(eq("user@test.com"), contains("예약이 확정"), anyString());
        assertThat(logCount("RESERVATION_CONFIRMED", "QUEUED")).isGreaterThanOrEqualTo(1);
    }

    @Test
    void 휴가반차_승인_시_담당매니저에게_통지() {
        // mgr1 ↔ manager1(manager@test.com) 연결됨(users.manager_id)
        DayoffApprovalResponse d = approvalService.submitDayoff(
                new DayoffRequest("mgr1", "2026-08-01", DayoffType.FULL_DAY));
        approvalService.approveDayoff(d.id());
        verify(emailSender).send(eq("manager@test.com"), contains("휴가/반차"), contains("승인"));
        assertThat(logCount("APPROVAL_RESULT", "QUEUED")).isGreaterThanOrEqualTo(1);
    }

    @Test
    void 미연결_매니저_휴가반차_승인은_SKIPPED_이력만() {
        // mgr2 ↔ 연결된 로그인 계정 없음 → 발송 skip, SKIPPED 이력만
        DayoffApprovalResponse d = approvalService.submitDayoff(
                new DayoffRequest("mgr2", "2026-08-02", DayoffType.FULL_DAY));
        approvalService.approveDayoff(d.id());
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
        assertThat(logCount("APPROVAL_RESULT", "SKIPPED")).isGreaterThanOrEqualTo(1);
    }

    @Test
    void 매니저_가입_2단계_승인_단계마다_통지() {
        // pending1(pending1@test.com, PENDING_APPROVAL_L1) → 1차 승인 → 2차 최종 승인
        signupApprovalService.approveL1("pending1");
        signupApprovalService.confirmL2("pending1");
        verify(emailSender, atLeast(2))
                .send(eq("pending1@test.com"), contains("매니저 가입"), anyString());
    }
}
