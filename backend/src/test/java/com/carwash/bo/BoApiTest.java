package com.carwash.bo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

// BO API 통합 테스트 (Phase 6) — 매니저 대행(M3)·관리자 매장 관리(S4·S5)·역할 인가(403/401)
@SpringBootTest
@AutoConfigureMockMvc
class BoApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private String bearer(String uid, UserRole role) {
        User user = User.builder().id(uid).email(uid + "@test.com").name("테스터").role(role).build();
        return "Bearer " + tokenProvider.createToken(user);
    }

    // 대행 요청 본문 — 고객 이메일/매니저/슬롯
    private String proxyBody(String managerId, String store, String bay, String date, String time) {
        return "{\"customerEmail\":\"user@test.com\",\"managerId\":\"" + managerId
                + "\",\"storeId\":\"" + store + "\",\"bayId\":\"" + bay + "\",\"date\":\"" + date
                + "\",\"timeSlot\":\"" + time
                + "\",\"carType\":\"SMALL\",\"serviceType\":\"EXT\",\"amount\":12000}";
    }

    @Test
    void 매니저_대행_성공_고객userId와_대행managerId가_기록된다() throws Exception {
        mvc.perform(post("/api/manager/reservations")
                        .header("Authorization", bearer("manager1", UserRole.MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(proxyBody("mgr1", "store1", "store1-A2", "2026-09-01", "09:00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user1"))
                .andExpect(jsonPath("$.managerId").value("mgr1"))
                .andExpect(jsonPath("$.status").value("RESERVED"));
    }

    @Test
    void 매니저_본인_휴무_시간대_대행은_400() throws Exception {
        // mgr1은 2026-06-22 FULL_DAY 휴무 → 해당 날짜 대행 차단
        mvc.perform(post("/api/manager/reservations")
                        .header("Authorization", bearer("manager1", UserRole.MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(proxyBody("mgr1", "store1", "store1-A2", "2026-06-22", "09:00")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 타_매장_매니저_대행은_400() throws Exception {
        // mgr3은 store2 소속 → store1 대행 불가
        mvc.perform(post("/api/manager/reservations")
                        .header("Authorization", bearer("manager1", UserRole.MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(proxyBody("mgr3", "store1", "store1-A2", "2026-09-02", "09:00")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void USER_역할_대행은_403() throws Exception {
        mvc.perform(post("/api/manager/reservations")
                        .header("Authorization", bearer("user1", UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(proxyBody("mgr1", "store1", "store1-A2", "2026-09-03", "09:00")))
                .andExpect(status().isForbidden());
    }

    @Test
    void 무토큰_대행은_401() throws Exception {
        mvc.perform(post("/api/manager/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(proxyBody("mgr1", "store1", "store1-A2", "2026-09-04", "09:00")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 관리자_매장별_예약자_조회_S4() throws Exception {
        // 대행 1건 생성(조회 대상 보장) 후 ADMIN 조회
        mvc.perform(post("/api/manager/reservations")
                        .header("Authorization", bearer("manager1", UserRole.MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(proxyBody("mgr1", "store1", "store1-A2", "2026-09-05", "09:00")))
                .andExpect(status().isOk());

        mvc.perform(get("/api/admin/stores/store1/reservations")
                        .header("Authorization", bearer("admin1", UserRole.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userEmail").exists())
                .andExpect(jsonPath("$[0].userName").exists());
    }

    @Test
    void 관리자_매장별_사용자_조회_S5() throws Exception {
        mvc.perform(post("/api/manager/reservations")
                        .header("Authorization", bearer("manager1", UserRole.MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(proxyBody("mgr1", "store1", "store1-A2", "2026-09-06", "09:00")))
                .andExpect(status().isOk());

        mvc.perform(get("/api/admin/stores/store1/users")
                        .header("Authorization", bearer("admin1", UserRole.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists());
    }

    @Test
    void MANAGER_역할_관리자_엔드포인트_접근은_403() throws Exception {
        mvc.perform(get("/api/admin/stores/store1/reservations")
                        .header("Authorization", bearer("manager1", UserRole.MANAGER)))
                .andExpect(status().isForbidden());
    }
}
