package com.carwash.approval;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carwash.domain.User;
import com.carwash.domain.enums.UserApprovalStatus;
import com.carwash.domain.enums.UserRole;
import com.carwash.mapper.UserMapper;
import com.carwash.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

// 매니저 가입 2단계 승인 통합 테스트 (require v1.7 §4.4) —
//   M7 STORE_ADMIN 1차 → S3 ADMIN 2차 → ACTIVE 로그인 / 권한 외 차단 / PENDING 로그인 거부 / 단계 건너뛰기 409.
//   테스트 격리: 각 케이스가 userMapper.insert로 고유 PENDING_APPROVAL_L1 매니저를 생성한다(시드 공유 상태 변경 회피).
@SpringBootTest
@AutoConfigureMockMvc
class SignupApprovalApiTest {

    // BCrypt('password') — 시드와 동일 해시
    private static final String HASH = "$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserMapper userMapper;

    private String token(String uid, UserRole role) {
        User u = User.builder().id(uid).email(uid + "@test.com").name("t").role(role).build();
        return "Bearer " + tokenProvider.createToken(u);
    }

    // 고유 PENDING_APPROVAL_L1 매니저 생성 → id 반환(email = id@test.com)
    private String createPendingManager(String suffix) {
        String id = "sgtest-" + suffix;
        User u = User.builder()
                .id(id).email(id + "@test.com").name("가입테스트").role(UserRole.MANAGER)
                .passwordHash(HASH).approvalStatus(UserApprovalStatus.PENDING_APPROVAL_L1).build();
        userMapper.insert(u);
        return id;
    }

    private String loginBody(String email) {
        return "{\"email\":\"" + email + "\",\"password\":\"password\"}";
    }

    @Test
    void 가입_2단계_승인_후_로그인_성공() throws Exception {
        String id = createPendingManager("ok");

        // 승인 전(PENDING) 로그인 거부 → 403
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(loginBody(id + "@test.com")))
                .andExpect(status().isForbidden());

        // 1차 승인(M7, STORE_ADMIN) → L2
        mvc.perform(patch("/api/store-admin/manager-signups/{id}/approve", id)
                        .header("Authorization", token("storeadmin1", UserRole.STORE_ADMIN)))
                .andExpect(status().isNoContent());

        // 2차 최종 승인(S3, ADMIN) → ACTIVE
        mvc.perform(patch("/api/admin/manager-approvals/{id}/confirm", id)
                        .header("Authorization", token("admin1", UserRole.ADMIN)))
                .andExpect(status().isNoContent());

        // ACTIVE 전환 후 로그인 성공
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(loginBody(id + "@test.com")))
                .andExpect(status().isOk());
    }

    @Test
    void 권한외_가입승인은_403() throws Exception {
        String id = createPendingManager("auth");

        // MANAGER가 1차 승인 시도 → 403(STORE_ADMIN 한정)
        mvc.perform(patch("/api/store-admin/manager-signups/{id}/approve", id)
                        .header("Authorization", token("manager1", UserRole.MANAGER)))
                .andExpect(status().isForbidden());

        // USER가 2차 confirm 시도 → 403(ADMIN 한정)
        mvc.perform(patch("/api/admin/manager-approvals/{id}/confirm", id)
                        .header("Authorization", token("user1", UserRole.USER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void 단계_건너뛰기_L1에서_2차_confirm_직접은_409() throws Exception {
        String id = createPendingManager("skip");

        // PENDING_APPROVAL_L1 상태에서 2차 confirm 직접 호출 → 불가 전이(409)
        mvc.perform(patch("/api/admin/manager-approvals/{id}/confirm", id)
                        .header("Authorization", token("admin1", UserRole.ADMIN)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
    }

    @Test
    void 반려된_가입은_로그인_거부() throws Exception {
        String id = createPendingManager("rej");

        // 매장매니저관리자 1차 반려 → REJECTED
        mvc.perform(patch("/api/store-admin/manager-signups/{id}/reject", id)
                        .header("Authorization", token("storeadmin1", UserRole.STORE_ADMIN)))
                .andExpect(status().isNoContent());

        // REJECTED 계정 로그인 거부 → 403
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(loginBody(id + "@test.com")))
                .andExpect(status().isForbidden());
    }
}
