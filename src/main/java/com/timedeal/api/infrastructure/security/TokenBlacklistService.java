package com.timedeal.api.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * JWT 토큰 블랙리스트 관리를 담당하는 서비스
 * 
 * @Service:
 * - Spring의 서비스 레이어를 나타내는 어노테이션
 * - 비즈니스 로직을 담당하는 클래스임을 명시
 * 
 * Redis 블랙리스트:
 * - 로그아웃된 토큰을 Redis에 저장하여 무효화
 * - 토큰 만료 시간까지 Redis에 보관
 * - 이후 요청에서 블랙리스트에 있는 토큰은 거부
 * 
 * 왜 Redis를 사용하는가?
 * - JWT는 stateless이므로 서버에서 토큰을 무효화할 수 없음
 * - 로그아웃된 토큰을 블랙리스트에 저장하여 추가 검증 필요
 * - Redis는 빠른 조회 성능과 TTL(Time To Live) 기능 제공
 */
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "blacklist:token:";
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 토큰을 블랙리스트에 추가
     * 
     * @param token: 블랙리스트에 추가할 JWT 토큰
     * @param expirationTimeInMillis: 토큰 만료 시간까지의 밀리초
     */
    public void addToBlacklist(String token, long expirationTimeInMillis) {
        String key = BLACKLIST_PREFIX + token;
        
        // 현재 시간부터 토큰 만료 시간까지의 남은 시간 계산
        long ttl = expirationTimeInMillis - System.currentTimeMillis();
        
        // TTL이 양수인 경우에만 Redis에 저장
        if (ttl > 0) {
            redisTemplate.opsForValue().set(key, "logout", ttl, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 토큰이 블랙리스트에 있는지 확인
     * 
     * @param token: 확인할 JWT 토큰
     * @return 블랙리스트에 있으면 true, 없으면 false
     */
    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
