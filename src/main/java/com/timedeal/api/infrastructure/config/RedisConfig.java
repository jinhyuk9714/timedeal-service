package com.timedeal.api.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 설정 클래스
 * 
 * @Configuration:
 * - Spring 설정 클래스임을 명시
 * - @Bean 메서드가 반환하는 객체를 Spring 빈으로 등록
 * 
 * Redis:
 * - 인메모리 데이터베이스
 * - JWT 토큰 블랙리스트 저장에 사용
 * - 빠른 조회 성능과 TTL(Time To Live) 기능 제공
 */
@Configuration
public class RedisConfig {

    /**
     * RedisTemplate 빈 등록
     * 
     * RedisTemplate:
     * - Spring Data Redis에서 제공하는 Redis 접근 템플릿
     * - Redis와의 상호작용을 위한 메서드 제공
     * 
     * StringRedisSerializer:
     * - 키와 값을 String으로 직렬화/역직렬화
     * - JWT 토큰은 String이므로 String 직렬화 사용
     * 
     * @param connectionFactory: Redis 연결 팩토리 (Spring Boot가 자동 주입)
     * @return RedisTemplate<String, String>
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 키와 값을 String으로 직렬화
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }
}
