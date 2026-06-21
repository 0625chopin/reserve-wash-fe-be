package com.carwash.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.carwash.service.EmailSender;

// 회원가입 이메일 인증 플로우 통합 테스트 — request → (dev-code 조회) → verify.
//   EmailSender 는 MockBean 으로 대체(실제 SMTP 미발송). 코드는 dev-code 백도어로 조회.
@SpringBootTest
@AutoConfigureMockMvc
class EmailVerificationApiTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper om;

    @MockBean private EmailSender emailSender;   // 실제 발송 차단(NotificationService → @Async send)

    private String peekCode(String email) throws Exception {
        String json = mvc.perform(get("/api/auth/signup/dev-code").param("email", email))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(json).get("code").asText();
    }

    @Test
    void USER_가입_인증_요청_검증_자동로그인() throws Exception {
        String email = "verify-user@test.com";
        mvc.perform(post("/api/auth/signup/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"pw12345\","
                                + "\"name\":\"인증유저\",\"role\":\"USER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresInSec").value(180));

        String code = peekCode(email);
        mvc.perform(post("/api/auth/signup/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.pendingApproval").value(false))
                .andExpect(jsonPath("$.user.role").value("USER"));
    }

    @Test
    void 매니저_가입_인증_검증_승인대기_토큰없음() throws Exception {
        String email = "verify-mgr@test.com";
        mvc.perform(post("/api/auth/signup/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"pw12345\","
                                + "\"name\":\"인증매니저\",\"role\":\"MANAGER\",\"storeId\":\"store1\"}"))
                .andExpect(status().isOk());

        String code = peekCode(email);
        mvc.perform(post("/api/auth/signup/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingApproval").value(true))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void 매니저_가입_매장없음_400() throws Exception {
        mvc.perform(post("/api/auth/signup/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nostore@test.com\",\"password\":\"pw12345\","
                                + "\"name\":\"무매장\",\"role\":\"MANAGER\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 이미_가입된_이메일_요청_409() throws Exception {
        mvc.perform(post("/api/auth/signup/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@test.com\",\"password\":\"pw12345\","
                                + "\"name\":\"중복\",\"role\":\"USER\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void 코드_불일치_400() throws Exception {
        String email = "wrongcode@test.com";
        mvc.perform(post("/api/auth/signup/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"pw12345\","
                                + "\"name\":\"불일치\",\"role\":\"USER\"}"))
                .andExpect(status().isOk());
        // 실제 코드와 다른 값
        mvc.perform(post("/api/auth/signup/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"code\":\"000000\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 재전송_후_새_코드로_검증() throws Exception {
        String email = "resend@test.com";
        mvc.perform(post("/api/auth/signup/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"pw12345\","
                                + "\"name\":\"재전송\",\"role\":\"USER\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/signup/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresInSec").value(180));

        String code = peekCode(email);
        mvc.perform(post("/api/auth/signup/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingApproval").value(false));
    }
}
