package com.carwash.security;

import com.carwash.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// JWT 발급/검증 — HS256 대칭키(jwt.secret), 만료 jwt.expiration-ms
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        // HS256은 256bit(32byte) 이상 키 필요 — application.yml 시크릿 길이 보장
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    // 사용자 정보를 클레임에 담아 토큰 발급 (subject=email, role/name/uid 클레임)
    public String createToken(User user) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .claim("name", user.getName())
                .claim("uid", user.getId())
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    // 서명 검증 + 파싱 — 무효/만료 시 예외(JwtException)
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
