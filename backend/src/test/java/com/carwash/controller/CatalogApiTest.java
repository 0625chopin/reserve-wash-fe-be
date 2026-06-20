package com.carwash.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

// 카탈로그 조회 API 통합 테스트 — 시드(data.sql) 로드된 컨텍스트에서 응답 계약 검증
@SpringBootTest
@AutoConfigureMockMvc
class CatalogApiTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void stores_승인_매장만_2건() throws Exception {
        // store3(판교점) approved=false 제외 (require 6.1)
        mvc.perform(get("/api/stores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].bayCount").isNumber());
    }

    @Test
    void managers_dayoffs_포함_isStoreAdmin_키_고정() throws Exception {
        // findAll ORDER BY id → 첫 매니저 mgr1(isStoreAdmin=true, dayoffs 3건)
        mvc.perform(get("/api/managers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].id").value("mgr1"))
                .andExpect(jsonPath("$[0].isStoreAdmin").value(true)) // 키가 storeAdmin이면 실패 → @JsonProperty 검증
                .andExpect(jsonPath("$[0].dayoffs.length()").value(3))
                .andExpect(jsonPath("$[0].dayoffs[0].type").exists());
    }

    @Test
    void bays_전체_9건_XLARGE_포함() throws Exception {
        mvc.perform(get("/api/bays"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(9))
                .andExpect(jsonPath("$[?(@.size=='XLARGE')]").exists());
    }

    @Test
    void prices_매트릭스_20건() throws Exception {
        mvc.perform(get("/api/prices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(20));
    }
}
