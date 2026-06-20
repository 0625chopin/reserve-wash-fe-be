package com.carwash.config;

import com.carwash.security.JwtAuthenticationFilter;
import com.carwash.security.JwtTokenProvider;
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
                .authorizeHttpRequests(auth -> auth
                        // 무인증 허용: 인증 API + 헬스 + 카탈로그 조회 + h2-console
                        .requestMatchers(
                                "/api/auth/**", "/api/health",
                                "/api/stores", "/api/managers", "/api/bays", "/api/prices")
                        .permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
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
