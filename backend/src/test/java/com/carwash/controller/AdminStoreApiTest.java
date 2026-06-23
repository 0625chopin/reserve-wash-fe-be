package com.carwash.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

// 관리자 매장 CRUD API 통합 테스트 (v2.4) — 등록(기본 미승인)·목록(미승인 포함)·인가(403)·삭제 무결성(409/204).
@SpringBootTest
@AutoConfigureMockMvc
class AdminStoreApiTest {

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

    // 베이 2개 동반 매장 등록 본문 — approved 미포함(기본 false 검증용). name은 호출자가 고유하게 지정.
    private String createBody(String name) {
        return "{\"name\":\"" + name + "\",\"bayCount\":2,\"bays\":["
                + "{\"code\":\"A1\",\"size\":\"SMALL\"},{\"code\":\"A2\",\"size\":\"LARGE\"}]}";
    }

    // ADMIN으로 매장 생성 → 서버 부여 id 반환
    private String createStore(String name) throws Exception {
        String json = mvc.perform(post("/api/admin/stores")
                        .header("Authorization", bearer("admin1", UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asText();
    }

    @Test
    void 매장_등록은_기본_미승인이고_베이가_함께_반영된다() throws Exception {
        String json = mvc.perform(post("/api/admin/stores")
                        .header("Authorization", bearer("admin1", UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("v2.4-등록기본미승인")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("v2.4-등록기본미승인"))
                .andExpect(jsonPath("$.approved").value(false)) // 🔒 approved 미입력 → false
                .andExpect(jsonPath("$.bays.length()").value(2))
                .andReturn().getResponse().getContentAsString();

        // 전역 H2를 공유하는 통합테스트라 생성분을 정리(CatalogApiTest의 베이/매장 카운트 단정 보호)
        String id = objectMapper.readTree(json).get("id").asText();
        mvc.perform(delete("/api/admin/stores/{id}", id)
                        .header("Authorization", bearer("admin1", UserRole.ADMIN)))
                .andExpect(status().isNoContent());
    }

    @Test
    void 관리자_목록은_미승인_매장도_포함하고_FO목록은_제외한다() throws Exception {
        // 미승인(approved=false) 매장을 새로 생성 → 관리자 목록엔 포함, FO /api/stores(승인만)엔 미노출
        String id = createStore("v2.4-미승인노출검증");

        mvc.perform(get("/api/admin/stores").header("Authorization", bearer("admin1", UserRole.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + id + "' && @.approved==false)]").exists());

        // FO 카탈로그(승인 매장만)에는 노출되지 않아야 한다(require 6.1)
        mvc.perform(get("/api/stores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + id + "')]").doesNotExist());

        // 정리: 무연관이므로 삭제
        mvc.perform(delete("/api/admin/stores/{id}", id)
                        .header("Authorization", bearer("admin1", UserRole.ADMIN)))
                .andExpect(status().isNoContent());
    }

    @Test
    void 비관리자_매장_등록은_403() throws Exception {
        mvc.perform(post("/api/admin/stores")
                        .header("Authorization", bearer("user1", UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("v2.4-비관리자")))
                .andExpect(status().isForbidden());
    }

    @Test
    void 연관_데이터_있는_매장_삭제는_409_STORE_HAS_DEPENDENCIES() throws Exception {
        // store1(강남점)은 예약/후기/매니저/슬롯 시드가 있어 삭제 차단(소프트 비활성 미채택)
        mvc.perform(delete("/api/admin/stores/{id}", "store1")
                        .header("Authorization", bearer("admin1", UserRole.ADMIN)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STORE_HAS_DEPENDENCIES"));
    }

    @Test
    void 무연관_매장_삭제는_204() throws Exception {
        String id = createStore("v2.4-삭제대상-무연관");
        mvc.perform(delete("/api/admin/stores/{id}", id)
                        .header("Authorization", bearer("admin1", UserRole.ADMIN)))
                .andExpect(status().isNoContent());
        // 삭제 후 단건 조회는 404
        mvc.perform(get("/api/admin/stores/{id}", id)
                        .header("Authorization", bearer("admin1", UserRole.ADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test
    void 매장_수정은_이름과_승인상태를_반영한다() throws Exception {
        String id = createStore("v2.4-수정전");
        String updateBody = "{\"name\":\"v2.4-수정후\",\"bayCount\":1,\"approved\":true,"
                + "\"bays\":[{\"code\":\"A1\",\"size\":\"MID\"}]}";
        mvc.perform(put("/api/admin/stores/{id}", id)
                        .header("Authorization", bearer("admin1", UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("v2.4-수정후"))
                .andExpect(jsonPath("$.approved").value(true))
                .andExpect(jsonPath("$.bays.length()").value(1));

        // 정리: 무연관이므로 삭제 가능
        mvc.perform(delete("/api/admin/stores/{id}", id)
                        .header("Authorization", bearer("admin1", UserRole.ADMIN)))
                .andExpect(status().isNoContent());
    }

    @Test
    void 미인증_매장_등록은_401() throws Exception {
        mvc.perform(post("/api/admin/stores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("v2.4-미인증")))
                .andExpect(status().isUnauthorized());
        assertTrue(true);
    }
}
