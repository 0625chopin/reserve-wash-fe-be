package com.carwash.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.carwash.domain.User;
import com.carwash.domain.enums.UserApprovalStatus;
import com.carwash.domain.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

// 사용자 매퍼 통합 테스트 — 비번 해시 조회 + 회원가입 insert (Phase 3)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    @Test
    void findByEmail_시드_비번해시_조회_matches() {
        User user = userMapper.findByEmail("user@test.com");

        assertThat(user).isNotNull();
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.getPasswordHash()).isNotBlank();
        // 시드 해시가 'password'와 일치하는지(BCrypt matches)
        assertThat(new BCryptPasswordEncoder().matches("password", user.getPasswordHash())).isTrue();
    }

    @Test
    void insert_후_재조회_성공() {
        User created = User.builder()
                .id("user-test-insert")
                .email("inserted@test.com")
                .name("삽입유저")
                .role(UserRole.USER)
                .passwordHash("$2a$10$dummdummdummdummdummduOeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee")
                .approvalStatus(UserApprovalStatus.ACTIVE)
                .build();

        int affected = userMapper.insert(created);
        assertThat(affected).isEqualTo(1);

        User found = userMapper.findByEmail("inserted@test.com");
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("삽입유저");
    }
}
