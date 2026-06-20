package com.carwash.config;

import com.carwash.security.JwtAuthenticationFilter;
import com.carwash.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

// Spring Security 설정 — stateless JWT, CORS 단일 소스(기존 CorsConfig 대체)
//   SecurityFilterChain은 서블릿 웹 컨텍스트에서만 생성 가능 → 비웹(NONE) 테스트 컨텍스트에선 비활성.
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityConfig {

    private final JwtTokenProvider tokenProvider;

    public SecurityConfig(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())                       // REST + JWT라 CSRF 미사용
                .cors(Customizer.withDefaults())                   // 아래 CorsConfigurationSource 사용
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(h -> h.frameOptions(f -> f.disable()))    // h2-console iframe 허용
                // 미인증 보호 API 접근은 403이 아닌 401로 응답(REST 관례, require 4장 DoD)
                .exceptionHandling(e -> e.authenticationEntryPoint(
                        (request, response, ex) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        // 무인증 허용: 인증 API + 헬스 + 카탈로그 조회 + 슬롯 점유 현황(공개) + h2-console
                        .requestMatchers(
                                "/api/auth/**", "/api/health",
                                "/api/stores", "/api/managers", "/api/bays", "/api/prices",
                                "/api/slots")
                        .permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        // BO 역할 인가 (require v1.7 §3.2·§8.3·§12.4) — 구체적 경로 매처를 anyRequest 이전에 둔다.
                        //   권한 외 역할 → 403(기본 AccessDeniedHandler), 미인증 → 401(위 entryPoint)
                        // 매장매니저관리자 전용(휴가/반차 승인 M8·가입 1차 승인 M7) — STORE_ADMIN 한정
                        .requestMatchers("/api/store-admin/**").hasRole("STORE_ADMIN")
                        // 일반매장매니저 영역(신청·대행 등) — MANAGER·STORE_ADMIN 공용
                        .requestMatchers("/api/manager/**").hasAnyRole("MANAGER", "STORE_ADMIN")
                        // 관리자 전용(가입 2차 승인 S3·매장휴일·매출 등) — ADMIN 한정
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(
                        new JwtAuthenticationFilter(tokenProvider),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // CORS 단일 소스 — FE(:3000) 교차 출처 허용(Authorization 헤더 포함, credentials)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
