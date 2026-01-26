package com.timedeal.api.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 토큰 생성 및 검증을 담당하는 클래스
 * 
 * @Component:
 * - Spring이 자동으로 빈으로 등록
 * - 다른 클래스에서 @Autowired로 주입 가능
 * 
 * JWT (JSON Web Token):
 * - 인증 정보를 안전하게 전달하기 위한 토큰 기반 인증 방식
 * - Header.Payload.Signature 구조로 구성
 * - 서버에서 서명하여 위조 방지
 */
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long tokenValidityInMilliseconds;

    /**
     * 생성자에서 JWT 설정값을 주입받음
     * 
     * @param secret: JWT 서명에 사용할 비밀키 (application.yml에서 설정)
     * @param tokenValidityInMilliseconds: 토큰 유효 기간 (밀리초)
     */
    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long tokenValidityInMilliseconds) {
        // 비밀키를 SecretKey 객체로 변환
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.tokenValidityInMilliseconds = tokenValidityInMilliseconds;
    }

    /**
     * 사용자 ID를 기반으로 JWT 토큰을 생성
     * 
     * @param userId: 사용자 ID
     * @return 생성된 JWT 토큰 문자열
     */
    public String generateToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + tokenValidityInMilliseconds);

        return Jwts.builder()
                .subject(String.valueOf(userId)) // 사용자 ID를 subject에 저장
                .issuedAt(now) // 토큰 발급 시간
                .expiration(expiryDate) // 토큰 만료 시간
                .signWith(secretKey) // 비밀키로 서명
                .compact(); // 최종 토큰 문자열 생성
    }

    /**
     * JWT 토큰에서 사용자 ID를 추출
     * 
     * @param token: JWT 토큰 문자열
     * @return 사용자 ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey) // 비밀키로 검증
                .build()
                .parseSignedClaims(token) // 토큰 파싱
                .getPayload(); // Payload 추출

        return Long.parseLong(claims.getSubject()); // subject에서 사용자 ID 추출
    }

    /**
     * JWT 토큰의 유효성을 검증
     * 
     * @param token: 검증할 JWT 토큰 문자열
     * @return 유효하면 true, 그렇지 않으면 false
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            // 토큰이 만료되었거나, 서명이 잘못되었거나, 형식이 잘못된 경우
            return false;
        }
    }

    /**
     * JWT 토큰의 만료 시간을 추출
     * 
     * @param token: JWT 토큰 문자열
     * @return 만료 시간 (밀리초)
     */
    public long getExpirationTime(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        return claims.getExpiration().getTime();
    }
}
