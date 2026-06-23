package com.carwash.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carwash.domain.User;
import com.carwash.domain.enums.UserRole;
import com.carwash.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

// 전 매장 매출 비중 집계 API 통합 테스트 (v2.4) — 매장별 COMPLETED 합계 내림차순·역할 인가.
@SpringBootTest
@AutoConfigureMockMvc
class SalesByStoreApiTest {

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

    @Test
    void 매장별_매출은_내림차순으로_반환된다() throws Exception {
        String json = mvc.perform(get("/api/admin/sales/by-store")
                        .header("Authorization", bearer("admin1", UserRole.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].storeId").exists())
                .andExpect(jsonPath("$[0].storeName").exists())
                .andExpect(jsonPath("$[0].amount").isNumber())
                .andReturn().getResponse().getContentAsString();

        JsonNode arr = objectMapper.readTree(json);
        // 시드 8개 매장 모두 포함
        org.junit.jupiter.api.Assertions.assertTrue(arr.size() >= 8, "전 매장 집계");
        // 금액 내림차순 보장
        long prev = Long.MAX_VALUE;
        for (JsonNode node : arr) {
            long amount = node.get("amount").asLong();
            assertTrue(amount <= prev, "매출은 내림차순이어야 함");
            prev = amount;
        }
    }

    @Test
    void USER_역할_매출_비중_조회는_403() throws Exception {
        mvc.perform(get("/api/admin/sales/by-store")
                        .header("Authorization", bearer("user1", UserRole.USER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void 미인증_매출_비중_조회는_401() throws Exception {
        mvc.perform(get("/api/admin/sales/by-store"))
                .andExpect(status().isUnauthorized());
    }
}
