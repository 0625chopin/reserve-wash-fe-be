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

// 예약 상태 전이 API 통합 테스트 (Phase 5) — 세차완료/취소·슬롯 전이·불가전이 409·비소유 404
@SpringBootTest
@AutoConfigureMockMvc
class ReservationTransitionApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    // 소유자(user1) / 타인(user2) 토큰
    private String bearer(String uid) {
        User user = User.builder().id(uid).email(uid + "@test.com").name("테스터").role(UserRole.USER).build();
        return "Bearer " + tokenProvider.createToken(user);
    }

    private String confirmBody(String bay, String date, String time) {
        return "{\"storeId\":\"store1\",\"bayId\":\"" + bay + "\",\"date\":\"" + date
                + "\",\"timeSlot\":\"" + time + "\",\"carType\":\"SMALL\",\"serviceType\":\"EXT\",\"amount\":12000}";
    }

    // 미점유 슬롯 확정 → 서버가 부여한 예약 id 반환
    private String createReservation(String uid, String bay, String date, String time) throws Exception {
        String json = mvc.perform(post("/api/reservations/confirm")
                        .header("Authorization", bearer(uid))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmBody(bay, date, time)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(json).get("id").asText();
    }

    // 특정 매장·날짜의 베이/시간 슬롯 상태 조회(없으면 null)
    private String slotStatus(String date, String bay, String time) throws Exception {
        String json = mvc.perform(get("/api/slots").param("storeId", "store1").param("date", date))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        for (var node : objectMapper.readTree(json)) {
            if (bay.equals(node.get("bayId").asText()) && time.equals(node.get("timeSlot").asText())) {
                return node.get("status").asText();
            }
        }
        return null;
    }

    @Test
    void 세차완료_RESERVED를_COMPLETED로_전이하고_슬롯도_COMPLETED() throws Exception {
        String id = createReservation("user1", "store1-A2", "2026-08-10", "09:00");

        mvc.perform(patch("/api/reservations/{id}/complete", id).header("Authorization", bearer("user1")))
                .andExpect(status().isNoContent());

        // 슬롯이 COMPLETED로 고정
        org.junit.jupiter.api.Assertions.assertEquals("COMPLETED", slotStatus("2026-08-10", "store1-A2", "09:00"));
    }

    @Test
    void 예약취소_CANCELED로_전이하고_슬롯이_AVAILABLE로_release() throws Exception {
        String id = createReservation("user1", "store1-A2", "2026-08-11", "09:00");

        mvc.perform(patch("/api/reservations/{id}/cancel", id).header("Authorization", bearer("user1")))
                .andExpect(status().isNoContent());

        // 슬롯이 다시 AVAILABLE(release) — 그리드 재진입 시 예약 가능
        org.junit.jupiter.api.Assertions.assertEquals("AVAILABLE", slotStatus("2026-08-11", "store1-A2", "09:00"));
    }

    @Test
    void 불가능한_전이_COMPLETED_재취소는_409() throws Exception {
        String id = createReservation("user1", "store1-A2", "2026-08-12", "09:00");
        mvc.perform(patch("/api/reservations/{id}/complete", id).header("Authorization", bearer("user1")))
                .andExpect(status().isNoContent());

        // 이미 COMPLETED → 취소 불가 → 409 INVALID_TRANSITION
        mvc.perform(patch("/api/reservations/{id}/cancel", id).header("Authorization", bearer("user1")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
    }

    @Test
    void 비소유자_전이_시도는_404() throws Exception {
        String id = createReservation("user1", "store1-A2", "2026-08-13", "09:00");

        // 타인(user2)이 전이 시도 → 404(존재 비노출)
        mvc.perform(patch("/api/reservations/{id}/cancel", id).header("Authorization", bearer("user2")))
                .andExpect(status().isNotFound());
    }

    @Test
    void 무토큰_전이는_401() throws Exception {
        mvc.perform(patch("/api/reservations/{id}/complete", "rsv-none"))
                .andExpect(status().isUnauthorized());
    }
}
