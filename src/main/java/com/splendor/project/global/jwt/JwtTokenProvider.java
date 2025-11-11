package com.splendor.project.global.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret-key}")
    private String secretKeyPlain;

    private Key secretKey;

    // 1000L * 60 * 60 * 24 = 24시간
    private static final long TOKEN_VALID_TIME = 24 * 60 * 60 * 1000L;

    @PostConstruct
    protected void init() {
        // yml에서 받은 String 시크릿 키를 Base64 인코딩하여 Key 객체로 변환
        String keyBase64 = Base64.getEncoder().encodeToString(secretKeyPlain.getBytes());
        secretKey = Keys.hmacShaKeyFor(keyBase64.getBytes());
    }

    /**
     * JWT 토큰 생성 (유저 ID와 닉네임을 Payload에 저장)
     */
    public String createToken(String userId, String nickname) {
        Claims claims = Jwts.claims().setSubject(userId); // Subject(sub)에 userId 저장
        claims.put("nickname", nickname); // "nickname"이라는 이름으로 닉네임 저장

        Date now = new Date();

        return Jwts.builder()
                .setClaims(claims) // 정보 저장
                .setIssuedAt(now) // 토큰 발행 시간
                .setExpiration(new Date(now.getTime() + TOKEN_VALID_TIME)) // 만료 시간
                .signWith(secretKey, SignatureAlgorithm.HS256) // HS256 암호화
                .compact();
    }

    /**
     * JWT 토큰 검증
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            System.out.println("Invalid JWT token: " + e.getMessage());
            return false;
        }
    }

    /**
     * 토큰에서 Payload(Claims) 추출
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 토큰에서 유저 ID (Subject) 추출
     */
    public String getUserIdFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    /**
     * 토큰에서 닉네임 추출
     */
    public String getNicknameFromToken(String token) {
        return (String) getClaimsFromToken(token).get("nickname");
    }
}
