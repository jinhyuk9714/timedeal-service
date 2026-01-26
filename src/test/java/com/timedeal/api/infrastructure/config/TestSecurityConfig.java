package com.timedeal.api.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 테스트용 Security 설정
 * 
 * 테스트 환경에서는 Security 필터를 완전히 제거하지만,
 * PasswordEncoder는 여전히 필요하므로 제공합니다.
 * 
 * @Profile("test"): test 프로파일에서만 활성화
 */
@Configuration
@Profile("test")
public class TestSecurityConfig {

    /**
     * 테스트에서 비밀번호 암호화를 위한 PasswordEncoder
     * Security 필터는 @AutoConfigureMockMvc(addFilters = false)로 제거되지만,
     * PasswordEncoder는 UserService에서 필요하므로 제공합니다.
     */
    @Bean
    @Primary
    public PasswordEncoder testPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
