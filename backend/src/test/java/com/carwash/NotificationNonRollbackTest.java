package com.carwash;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.carwash.mapper.UserMapper;
import com.carwash.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

// Phase 9 비동기 비롤백 검증 — JavaMailSender를 MockBean으로 두고 실제 SmtpEmailSender(@Async)가 호출하게 한다.
//   발송이 실패(MailSendException)해도 그것은 별도 비동기 스레드라, 가입(@Transactional)은 커밋된다.
//   커밋된 DB 상태(사용자 영속)를 단정하므로 비동기 완료를 기다릴 필요가 없는 결정적 테스트다.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class NotificationNonRollbackTest {

    @MockBean
    private JavaMailSender mailSender;

    @Autowired private AuthService authService;
    @Autowired private UserMapper userMapper;

    @Test
    void 메일_발송이_실패해도_가입_트랜잭션은_커밋된다() {
        // 비동기 발송 시 SMTP 실패 주입(별도 스레드에서 던져짐 → 호출자 트랜잭션에 비전파)
        doThrow(new MailSendException("SMTP unavailable"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        authService.signup("rollbacktest@test.com", "password", "비롤백테스트");

        // 발송 실패와 무관하게 가입은 커밋되어 사용자가 영속됨
        assertThat(userMapper.findByEmail("rollbacktest@test.com")).isNotNull();
    }
}
