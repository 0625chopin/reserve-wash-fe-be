package com.carwash.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

// 슬롯 점유 현황 조회 API 테스트 — permitAll(무인증)
@SpringBootTest
@AutoConfigureMockMvc
class SlotApiTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void slots_시드_점유_슬롯_조회() throws Exception {
        // 시드: store1 A1 2026-06-25 10:00 = RESERVED
        mvc.perform(get("/api/slots").param("storeId", "store1").param("date", "2026-06-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].bayId").value("store1-A1"))
                .andExpect(jsonPath("$[0].status").value("RESERVED"));
    }

    @Test
    void slots_점유_없는_날짜_빈배열() throws Exception {
        mvc.perform(get("/api/slots").param("storeId", "store1").param("date", "2099-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
