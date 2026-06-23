package com.carwash.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void stores_전_매장_승인_8건() throws Exception {
        // 전 매장 approved=true → 8개 모두 노출 (require 6.1)
        mvc.perform(get("/api/stores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8))
                .andExpect(jsonPath("$[0].bayCount").isNumber());
    }

    @Test
    void managers_dayoffs_포함_isStoreAdmin_키_고정() throws Exception {
        // mgr1(isStoreAdmin=true, dayoffs 3건)을 id로 찾아 검증한다.
        //   ⚠ 전역 H2를 공유하는 통합테스트라 매니저 가입 최종 승인(v2.4)으로 런타임 manager 엔티티가
        //      추가될 수 있어, 개수(==12)·첫 요소($[0]) 고정 대신 mgr1을 파싱해 견고하게 단정한다.
        String json = mvc.perform(get("/api/managers"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode arr = objectMapper.readTree(json);
        JsonNode mgr1 = null;
        for (JsonNode n : arr) {
            if ("mgr1".equals(n.get("id").asText())) {
                mgr1 = n;
                break;
            }
        }
        assertThat(mgr1).as("시드 mgr1 존재").isNotNull();
        assertThat(mgr1.has("isStoreAdmin")).as("@JsonProperty 키(storeAdmin 아님)").isTrue();
        assertThat(mgr1.get("isStoreAdmin").asBoolean()).isTrue();
        assertThat(mgr1.get("dayoffs").size()).isEqualTo(3);
        assertThat(mgr1.get("dayoffs").get(0).get("type").asText()).isNotBlank();
    }

    @Test
    void bays_전체_25건_XLARGE_포함() throws Exception {
        mvc.perform(get("/api/bays"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(25))
                .andExpect(jsonPath("$[?(@.size=='XLARGE')]").exists());
    }

    @Test
    void prices_매트릭스_20건() throws Exception {
        mvc.perform(get("/api/prices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(20));
    }
}
