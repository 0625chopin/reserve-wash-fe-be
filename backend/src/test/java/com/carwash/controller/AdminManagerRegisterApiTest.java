package com.carwash.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carwash.domain.User;
import com.carwash.domain.enums.UserRole;
import com.carwash.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

// 관리자 직접 매니저 등록 API 통합 테스트 (require v1.12 §4.1) — POST /api/admin/managers
//   L2 생성·최종승인 목록 합류·승인 후 로그인·역할 검증(400)·중복(409)·인가(403/401)를 검증한다.
@SpringBootTest
@AutoConfigureMockMvc
class AdminManagerRegisterApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private String bearer(String uid, UserRole role) {
        User user = User.builder().id(uid).email(uid + "@test.com").name("테스터").role(role).build();
        return "Bearer " + tokenProvider.createToken(user);
    }

    private String body(String name, String email, String password, String storeId, String role) {
        return "{\"name\":\"" + name + "\",\"email\":\"" + email + "\",\"password\":\"" + password
                + "\",\"storeId\":\"" + storeId + "\",\"role\":\"" + role + "\"}";
    }

    private String adminToken() {
        return bearer("admin1", UserRole.ADMIN);
    }

    @Test
    void 관리자_매니저_등록은_L2로_생성되고_승인후_로그인된다() throws Exception {
        // 등록 → PENDING_APPROVAL_L2, role MANAGER
        String json = mvc.perform(post("/api/admin/managers")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("등록매니저", "reg-mgr-1@test.com", "secret123", "store1", "MANAGER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("MANAGER"))
                .andExpect(jsonPath("$.approvalStatus").value("PENDING_APPROVAL_L2"))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(json).get("id").asText();

        // 최종 승인 목록(PENDING_APPROVAL_L2)에 합류
        mvc.perform(get("/api/admin/manager-approvals").header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + id + "')]").exists());

        // 승인 전 로그인 불가(403)
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"reg-mgr-1@test.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isForbidden());

        // 2차 최종 승인 → ACTIVE
        mvc.perform(patch("/api/admin/manager-approvals/" + id + "/confirm").header("Authorization", adminToken()))
                .andExpect(status().isNoContent());

        // 승인 후 로그인 성공
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"reg-mgr-1@test.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("MANAGER"));
    }

    @Test
    void 매장관리매니저_STORE_ADMIN도_등록할_수_있다() throws Exception {
        String json = mvc.perform(post("/api/admin/managers")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("등록관리자", "reg-sadm-1@test.com", "secret123", "store1", "STORE_ADMIN")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STORE_ADMIN"))
                .andExpect(jsonPath("$.approvalStatus").value("PENDING_APPROVAL_L2"))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(json).get("id").asText();

        mvc.perform(get("/api/admin/manager-approvals").header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + id + "' && @.role=='STORE_ADMIN')]").exists());
    }

    @Test
    void USER_역할_등록은_400() throws Exception {
        mvc.perform(post("/api/admin/managers")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("일반", "reg-user-1@test.com", "secret123", "store1", "USER")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 중복_이메일_등록은_409() throws Exception {
        // 시드 매니저 이메일(manager@test.com) 재사용 → 409
        mvc.perform(post("/api/admin/managers")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("중복", "manager@test.com", "secret123", "store1", "MANAGER")))
                .andExpect(status().isConflict());
    }

    @Test
    void 비관리자_등록은_403() throws Exception {
        mvc.perform(post("/api/admin/managers")
                        .header("Authorization", bearer("manager1", UserRole.MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("권한없음", "reg-forbidden@test.com", "secret123", "store1", "MANAGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void 미인증_등록은_401() throws Exception {
        mvc.perform(post("/api/admin/managers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("미인증", "reg-unauth@test.com", "secret123", "store1", "MANAGER")))
                .andExpect(status().isUnauthorized());
    }
}
