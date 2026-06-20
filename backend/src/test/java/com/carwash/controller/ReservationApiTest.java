package com.carwash.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carwash.domain.User;
import com.carwash.domain.enums.UserRole;
import com.carwash.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

// 예약 API 통합 테스트 — 보호 API(JWT) + 충돌 409
@SpringBootTest
@AutoConfigureMockMvc
class ReservationApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private String bearer() {
        User user = User.builder().id("user1").email("user@test.com").name("홍길동").role(UserRole.USER).build();
        return "Bearer " + tokenProvider.createToken(user);
    }

    private String confirmBody(String bay, String date, String time) {
        return "{\"storeId\":\"store1\",\"bayId\":\"" + bay + "\",\"date\":\"" + date
                + "\",\"timeSlot\":\"" + time + "\",\"carType\":\"SMALL\",\"serviceType\":\"EXT\",\"amount\":12000}";
    }

    @Test
    void confirm_무토큰_401() throws Exception {
        mvc.perform(post("/api/reservations/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmBody("store1-A2", "2026-08-01", "09:00")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void confirm_성공_후_동일슬롯_재확정_409() throws Exception {
        // 미점유 슬롯 확정 → 성공(RESERVED)
        mvc.perform(post("/api/reservations/confirm")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmBody("store1-A2", "2026-08-02", "09:00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESERVED"))
                .andExpect(jsonPath("$.userId").value("user1"));

        // 동일 슬롯 재확정 → uk_slot 위반 → 409
        mvc.perform(post("/api/reservations/confirm")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmBody("store1-A2", "2026-08-02", "09:00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_CONFLICT"));
    }
}
