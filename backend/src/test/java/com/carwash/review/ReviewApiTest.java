package com.carwash.review;

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

// 후기/매출 API 통합 테스트 (Phase 8) — 자격가드·중복·평점범위·평균·매출(S8)·역할 인가
@SpringBootTest
@AutoConfigureMockMvc
class ReviewApiTest {

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

    private String confirmBody(String bay, String date, String time) {
        return "{\"storeId\":\"store1\",\"bayId\":\"" + bay + "\",\"date\":\"" + date
                + "\",\"timeSlot\":\"" + time + "\",\"carType\":\"SMALL\",\"serviceType\":\"EXT\",\"amount\":12000}";
    }

    // user1 예약 확정 → 서버 예약 id 반환
    private String confirm(String bay, String date, String time) throws Exception {
        String json = mvc.perform(post("/api/reservations/confirm")
                        .header("Authorization", token("user1", UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmBody(bay, date, time)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asText();
    }

    private void complete(String reservationId) throws Exception {
        mvc.perform(patch("/api/reservations/{id}/complete", reservationId)
                        .header("Authorization", token("user1", UserRole.USER)))
                .andExpect(status().isNoContent());
    }

    private String reviewBody(String reservationId, int rating) {
        return "{\"reservationId\":\"" + reservationId + "\",\"rating\":" + rating + ",\"text\":\"좋아요\"}";
    }

    @Test
    void 세차완료_예약에_후기_작성_성공() throws Exception {
        String id = confirm("store1-A2", "2026-12-10", "09:00");
        complete(id);
        mvc.perform(post("/api/reviews")
                        .header("Authorization", token("user1", UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewBody(id, 5)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.storeId").value("store1"));
    }

    @Test
    void 중복_후기_작성은_409() throws Exception {
        String id = confirm("store1-A2", "2026-12-11", "09:00");
        complete(id);
        mvc.perform(post("/api/reviews").header("Authorization", token("user1", UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON).content(reviewBody(id, 4)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/reviews").header("Authorization", token("user1", UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON).content(reviewBody(id, 3)))
                .andExpect(status().isConflict());
    }

    @Test
    void 미완료_예약_후기_작성은_400() throws Exception {
        String id = confirm("store1-A2", "2026-12-12", "09:00");   // complete 안 함 → RESERVED
        mvc.perform(post("/api/reviews").header("Authorization", token("user1", UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON).content(reviewBody(id, 5)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 타인_예약_후기_작성은_404() throws Exception {
        String id = confirm("store1-A2", "2026-12-13", "09:00");
        complete(id);
        mvc.perform(post("/api/reviews").header("Authorization", token("user2", UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON).content(reviewBody(id, 5)))
                .andExpect(status().isNotFound());
    }

    @Test
    void 평점_범위_초과는_400() throws Exception {
        String id = confirm("store1-A2", "2026-12-14", "09:00");
        complete(id);
        mvc.perform(post("/api/reviews").header("Authorization", token("user1", UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON).content(reviewBody(id, 6)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 매장_평균_평점_조회() throws Exception {
        String id = confirm("store1-A2", "2026-12-15", "09:00");
        complete(id);
        mvc.perform(post("/api/reviews").header("Authorization", token("user1", UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON).content(reviewBody(id, 5)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/reviews/stores/store1/average")
                        .header("Authorization", token("user1", UserRole.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").isNumber())
                .andExpect(jsonPath("$.average").isNumber());
    }

    @Test
    void 관리자_매출_집계_S8() throws Exception {
        String id = confirm("store1-A2", "2026-12-16", "09:00");
        complete(id);
        mvc.perform(get("/api/admin/stores/store1/sales")
                        .header("Authorization", token("admin1", UserRole.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeId").value("store1"))
                .andExpect(jsonPath("$.total").isNumber());
    }

    @Test
    void USER_역할_매출_조회는_403() throws Exception {
        mvc.perform(get("/api/admin/stores/store1/sales")
                        .header("Authorization", token("user1", UserRole.USER)))
                .andExpect(status().isForbidden());
    }
}
