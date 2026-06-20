package com.carwash.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// 웹 여부와 무관하게 항상 로드되는 빈 — PasswordEncoder는 비웹(NONE) 컨텍스트(매퍼 테스트)에서도
// AuthService 주입에 필요하므로 SecurityConfig(웹 전용)와 분리해 둔다.
@Configuration
public class AppBeansConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
