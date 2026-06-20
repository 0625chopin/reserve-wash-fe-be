package com.carwash.approval;

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

// 결재 상태머신 통합 테스트 (require v1.7 §8) — 휴가/반차 1단계(STORE_ADMIN 종결)·휴일 1단계·반려/재신청·역할·슬롯 비활성
@SpringBootTest
@AutoConfigureMockMvc
class ApprovalApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private String token(String uid, UserRole role) {
        User u = User.builder().id(uid).email(uid + "@test.com").name("t").role(role).build();
        return "Bearer " + tokenProvider.createToken(u);
    }

    private String dayoffBody(String managerId, String date, String type) {
        return "{\"managerId\":\"" + managerId + "\",\"date\":\"" + date + "\",\"type\":\"" + type + "\"}";
    }

    // 휴무 신청 → 생성된 id 반환
    private long submitDayoff(String managerId, String date, String type) throws Exception {
        String json = mvc.perform(post("/api/manager/dayoffs")
                        .header("Authorization", token("manager1", UserRole.MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dayoffBody(managerId, date, type)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }

    // mgr4 소속 매장(store3) 기준 대행 본문 — 확정 휴무가 대행을 차단하는지 검증용
    private String proxyBody(String managerId, String date, String time) {
        return "{\"customerEmail\":\"user@test.com\",\"managerId\":\"" + managerId
                + "\",\"storeId\":\"store3\",\"bayId\":\"store3-A1\",\"date\":\"" + date
                + "\",\"timeSlot\":\"" + time
                + "\",\"carType\":\"SMALL\",\"serviceType\":\"EXT\",\"amount\":12000}";
    }

    @Test
    void 휴가반차_1단계_승인_SUBMITTED에서_APPROVED_종결() throws Exception {
        long id = submitDayoff("mgr4", "2026-11-01", "FULL_DAY");

        // MANAGER는 승인 불가(STORE_ADMIN 한정) → 403
        mvc.perform(patch("/api/store-admin/dayoffs/{id}/approve", id)
                        .header("Authorization", token("manager1", UserRole.MANAGER)))
                .andExpect(status().isForbidden());

        // 관리자(ADMIN)도 휴가/반차 승인 권한 없음(v1.7: 관리자 미개입) → 403
        mvc.perform(patch("/api/store-admin/dayoffs/{id}/approve", id)
                        .header("Authorization", token("admin1", UserRole.ADMIN)))
                .andExpect(status().isForbidden());

        // 매장매니저관리자(STORE_ADMIN) 승인 → APPROVED 종결(204)
        mvc.perform(patch("/api/store-admin/dayoffs/{id}/approve", id)
                        .header("Authorization", token("storeadmin1", UserRole.STORE_ADMIN)))
                .andExpect(status().isNoContent());
    }

    @Test
    void 이미_승인된_휴가반차_재승인은_409() throws Exception {
        long id = submitDayoff("mgr4", "2026-11-02", "FULL_DAY");
        // 1단계 승인 종결
        mvc.perform(patch("/api/store-admin/dayoffs/{id}/approve", id)
                        .header("Authorization", token("storeadmin1", UserRole.STORE_ADMIN)))
                .andExpect(status().isNoContent());
        // APPROVED 상태에서 재승인 시도 → 불가 전이(409)
        mvc.perform(patch("/api/store-admin/dayoffs/{id}/approve", id)
                        .header("Authorization", token("storeadmin1", UserRole.STORE_ADMIN)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
    }

    @Test
    void 반려_후_재신청() throws Exception {
        long id = submitDayoff("mgr4", "2026-11-03", "FULL_DAY");
        // 매장매니저관리자 반려 → REJECTED
        mvc.perform(patch("/api/store-admin/dayoffs/{id}/reject", id)
                        .header("Authorization", token("storeadmin1", UserRole.STORE_ADMIN)))
                .andExpect(status().isNoContent());
        // 매니저 재신청 → SUBMITTED
        mvc.perform(patch("/api/manager/dayoffs/{id}/resubmit", id)
                        .header("Authorization", token("manager1", UserRole.MANAGER)))
                .andExpect(status().isNoContent());
    }

    @Test
    void 휴일_1단계_승인() throws Exception {
        String json = mvc.perform(post("/api/manager/holidays")
                        .header("Authorization", token("manager1", UserRole.MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeId\":\"store1\",\"date\":\"2026-11-20\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(json).get("id").asLong();

        mvc.perform(patch("/api/admin/holidays/{id}/approve", id)
                        .header("Authorization", token("admin1", UserRole.ADMIN)))
                .andExpect(status().isNoContent());
    }

    @Test
    void USER_역할_휴무신청은_403() throws Exception {
        mvc.perform(post("/api/manager/dayoffs")
                        .header("Authorization", token("user1", UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dayoffBody("mgr4", "2026-11-04", "FULL_DAY")))
                .andExpect(status().isForbidden());
    }

    @Test
    void 확정_FULL_DAY_휴무는_그날_대행을_차단한다() throws Exception {
        // mgr4(store3) FULL_DAY 2026-12-01 신청 → STORE_ADMIN 1단계 승인 APPROVED 종결
        long id = submitDayoff("mgr4", "2026-12-01", "FULL_DAY");
        mvc.perform(patch("/api/store-admin/dayoffs/{id}/approve", id)
                        .header("Authorization", token("storeadmin1", UserRole.STORE_ADMIN)))
                .andExpect(status().isNoContent());

        // 승인된 휴무가 카탈로그에 반영 → mgr4 대행이 그날 전체 차단(400)
        mvc.perform(post("/api/manager/reservations")
                        .header("Authorization", token("manager1", UserRole.MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(proxyBody("mgr4", "2026-12-01", "09:00")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 확정_SHIFT1_휴무는_오전조만_차단하고_오후는_허용한다() throws Exception {
        // mgr4(store3) SHIFT_1(06:00~14:00) 2026-12-02 → STORE_ADMIN 1단계 승인 APPROVED 종결
        long id = submitDayoff("mgr4", "2026-12-02", "SHIFT_1");
        mvc.perform(patch("/api/store-admin/dayoffs/{id}/approve", id)
                        .header("Authorization", token("storeadmin1", UserRole.STORE_ADMIN)))
                .andExpect(status().isNoContent());

        // 오전(09:00) 대행 → 차단(400)
        mvc.perform(post("/api/manager/reservations")
                        .header("Authorization", token("manager1", UserRole.MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(proxyBody("mgr4", "2026-12-02", "09:00")))
                .andExpect(status().isBadRequest());

        // 오후(15:00, SHIFT_2) 대행 → 허용(200)
        mvc.perform(post("/api/manager/reservations")
                        .header("Authorization", token("manager1", UserRole.MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(proxyBody("mgr4", "2026-12-02", "15:00")))
                .andExpect(status().isOk());
    }
}
