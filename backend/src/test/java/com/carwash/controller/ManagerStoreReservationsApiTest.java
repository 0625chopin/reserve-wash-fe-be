package com.carwash.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carwash.domain.User;
import com.carwash.domain.enums.UserRole;
import com.carwash.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

// 역할별 예약 목록 API 통합 테스트 (require v1.10 §6.6)
//   · GET /api/manager/reservations      — 담당 예약(managerId 기준). 본인 userId 예약과 분리.
//   · GET /api/store-admin/reservations  — 매장 전체 예약(storeId 기준).
//   소속/인가(403)·미인증(401)·managerId 미연결 빈 목록을 검증한다.
@SpringBootTest
@AutoConfigureMockMvc
class ManagerStoreReservationsApiTest {

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

    // 매니저를 지정한 일반 예약 — managerId를 명시해 confirm
    private String confirmBody(String store, String bay, String date, String time, String managerId) {
        return "{\"storeId\":\"" + store + "\",\"bayId\":\"" + bay + "\",\"date\":\"" + date
                + "\",\"timeSlot\":\"" + time + "\",\"managerId\":\"" + managerId
                + "\",\"carType\":\"SMALL\",\"serviceType\":\"EXT\",\"amount\":12000}";
    }

    private String createReservation(
            String uid, String store, String bay, String date, String time, String managerId) throws Exception {
        String json = mvc.perform(post("/api/reservations/confirm")
                        .header("Authorization", bearer(uid, UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmBody(store, bay, date, time, managerId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asText();
    }

    private JsonNode getList(String path, String uid, UserRole role) throws Exception {
        String json = mvc.perform(get(path).header("Authorization", bearer(uid, role)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json);
    }

    private List<String> idsOf(JsonNode arr) {
        List<String> ids = new ArrayList<>();
        for (var node : arr) {
            ids.add(node.get("id").asText());
        }
        return ids;
    }

    @Test
    void 담당_예약_목록은_managerId_기준으로_반환한다() throws Exception {
        // r1: 고객 user1이 매니저 mgr1을 지정한 예약 / r2: user2가 mgr2를 지정한 예약(같은 매장)
        //   슬롯은 공유 H2 전역 자원 → 타 테스트와 겹치지 않는 고유 날짜(2026-09-20) 사용
        String r1 = createReservation("user1", "store1", "store1-A2", "2026-09-20", "09:00", "mgr1");
        String r2 = createReservation("user2", "store1", "store1-A3", "2026-09-20", "09:00", "mgr2");

        // manager1(↔mgr1) 담당 목록: mgr1 귀속 예약만, mgr2 귀속은 제외 + 모든 항목 managerId=mgr1
        JsonNode arr = getList("/api/manager/reservations", "manager1", UserRole.MANAGER);
        List<String> ids = idsOf(arr);
        assertTrue(ids.contains(r1), "담당(mgr1) 예약은 포함되어야 함");
        assertFalse(ids.contains(r2), "타 매니저(mgr2) 예약은 제외되어야 함");
        for (var node : arr) {
            assertEquals("mgr1", node.get("managerId").asText(), "담당 목록은 모두 managerId=mgr1");
        }
    }

    @Test
    void 매장매니저관리자_매장_전체_예약을_반환한다() throws Exception {
        // store1 예약(mgr1) + store2 예약(mgr3) — 고유 날짜(2026-09-21)로 슬롯 충돌 회피
        String inStore = createReservation("user1", "store1", "store1-A2", "2026-09-21", "09:00", "mgr1");
        String otherStore = createReservation("user1", "store2", "store2-A1", "2026-09-21", "09:00", "mgr3");

        // storeadmin1(store1) 매장 전체 목록: store1 예약 포함, store2 제외 + 모든 항목 storeId=store1
        JsonNode arr = getList("/api/store-admin/reservations", "storeadmin1", UserRole.STORE_ADMIN);
        List<String> ids = idsOf(arr);
        assertTrue(ids.contains(inStore), "소속 매장(store1) 예약은 포함되어야 함");
        assertFalse(ids.contains(otherStore), "타 매장(store2) 예약은 제외되어야 함");
        for (var node : arr) {
            assertEquals("store1", node.get("storeId").asText(), "매장 전체 목록은 모두 storeId=store1");
        }
    }

    @Test
    void managerId_미연결_매니저의_담당_목록은_빈_배열() throws Exception {
        // storeadmin1은 manager_id NULL(매장 전체를 store_id로 조회) → 담당 목록은 빈 배열(200)
        JsonNode arr = getList("/api/manager/reservations", "storeadmin1", UserRole.STORE_ADMIN);
        assertTrue(arr.isArray() && arr.isEmpty(), "managerId 미연결이면 담당 목록은 빈 배열");
    }

    @Test
    void USER는_담당_예약_목록을_조회할_수_없다_403() throws Exception {
        mvc.perform(get("/api/manager/reservations").header("Authorization", bearer("user1", UserRole.USER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void 매장_전체_예약은_STORE_ADMIN만_조회한다_USER_MANAGER_403() throws Exception {
        mvc.perform(get("/api/store-admin/reservations").header("Authorization", bearer("user1", UserRole.USER)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/store-admin/reservations").header("Authorization", bearer("manager1", UserRole.MANAGER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void 미인증_담당_조회는_401() throws Exception {
        mvc.perform(get("/api/manager/reservations"))
                .andExpect(status().isUnauthorized());
    }
}
