package com.carwash.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carwash.domain.User;
import com.carwash.domain.enums.UserRole;
import com.carwash.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

// 본인 예약 목록 API 통합 테스트 (BUG-1) — GET /api/reservations
//   소유 격리(본인 것만)·대행 예약 포함(userId=고객)·미인증 401을 검증한다.
@SpringBootTest
@AutoConfigureMockMvc
class MyReservationsApiTest {

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

    private String confirmBody(String bay, String date, String time) {
        return "{\"storeId\":\"store1\",\"bayId\":\"" + bay + "\",\"date\":\"" + date
                + "\",\"timeSlot\":\"" + time + "\",\"carType\":\"SMALL\",\"serviceType\":\"EXT\",\"amount\":12000}";
    }

    // 본인 확정 → 서버 부여 예약 id 반환
    private String createReservation(String uid, String bay, String date, String time) throws Exception {
        String json = mvc.perform(post("/api/reservations/confirm")
                        .header("Authorization", bearer(uid, UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmBody(bay, date, time)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asText();
    }

    // GET /api/reservations 로 받은 내 예약들의 id 집합
    private List<String> myReservationIds(String uid) throws Exception {
        String json = mvc.perform(get("/api/reservations").header("Authorization", bearer(uid, UserRole.USER)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> ids = new ArrayList<>();
        for (var node : objectMapper.readTree(json)) {
            // 응답의 모든 항목은 호출자 소유여야 한다(서버측 userId 격리)
            org.junit.jupiter.api.Assertions.assertEquals(uid, node.get("userId").asText());
            ids.add(node.get("id").asText());
        }
        return ids;
    }

    @Test
    void 본인_예약만_반환하고_타인_예약은_제외한다() throws Exception {
        String mine = createReservation("user1", "store1-A2", "2026-08-20", "09:00");
        String others = createReservation("user2", "store1-A2", "2026-08-20", "10:00");

        List<String> user1Ids = myReservationIds("user1");
        assertTrue(user1Ids.contains(mine), "본인 예약은 목록에 포함되어야 함");
        assertFalse(user1Ids.contains(others), "타인 예약은 목록에서 제외되어야 함");

        // 대칭: user2 목록엔 user2 것만
        List<String> user2Ids = myReservationIds("user2");
        assertTrue(user2Ids.contains(others));
        assertFalse(user2Ids.contains(mine));
    }

    @Test
    void 매니저_대행_예약도_고객_목록에_포함된다() throws Exception {
        // 매니저(mgr2, store1, 휴무없음)가 고객 user@test.com(=user1) 대행 예약
        String proxyBody = "{\"customerEmail\":\"user@test.com\",\"managerId\":\"mgr2\",\"storeId\":\"store1\","
                + "\"bayId\":\"store1-A2\",\"date\":\"2026-08-21\",\"timeSlot\":\"09:00\","
                + "\"carType\":\"MID\",\"serviceType\":\"FULL\",\"amount\":27000}";
        String json = mvc.perform(post("/api/manager/reservations")
                        .header("Authorization", bearer("manager1", UserRole.MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(proxyBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String proxyId = objectMapper.readTree(json).get("id").asText();

        // 고객(user1) 본인 목록에 대행 예약이 보여야 한다(헤드라인 결함 해소)
        assertTrue(myReservationIds("user1").contains(proxyId), "대행 예약이 고객 목록에 포함되어야 함");
    }

    @Test
    void 미인증_조회는_401() throws Exception {
        mvc.perform(get("/api/reservations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 응답_항목은_FE_표시_필드를_갖는다() throws Exception {
        createReservation("user1", "store1-A3", "2026-08-22", "11:00");
        mvc.perform(get("/api/reservations").header("Authorization", bearer("user1", UserRole.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].storeId").exists())
                .andExpect(jsonPath("$[0].bayId").exists())
                .andExpect(jsonPath("$[0].date").exists())
                .andExpect(jsonPath("$[0].timeSlot").exists())
                .andExpect(jsonPath("$[0].carType").exists())
                .andExpect(jsonPath("$[0].serviceType").exists())
                .andExpect(jsonPath("$[0].amount").exists())
                .andExpect(jsonPath("$[0].status").exists());
    }
}
