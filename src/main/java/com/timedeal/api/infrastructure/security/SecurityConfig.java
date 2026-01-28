package com.timedeal.api.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 설정 클래스
 * 
 * @Configuration:
 * - Spring 설정 클래스임을 명시
 * - @Bean 메서드가 반환하는 객체를 Spring 빈으로 등록
 * 
 * @EnableWebSecurity:
 * - Spring Security의 웹 보안 기능 활성화
 * - 기본 보안 설정을 커스터마이징 가능
 * 
 * @Profile("!test"):
 * - test 프로파일이 아닐 때만 활성화
 * - 테스트 환경에서는 TestSecurityConfig가 사용됨
 * 
 * SecurityFilterChain:
 * - HTTP 요청에 대한 보안 규칙을 정의
 * - 인증/인가 설정, 필터 체인 구성
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // @PreAuthorize 어노테이션 활성화
@RequiredArgsConstructor
@Profile("!test")
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Spring Security의 필터 체인 설정
     * 
     * @param http: HttpSecurity 객체 (보안 설정을 위한 빌더)
     * @return SecurityFilterChain (설정된 보안 필터 체인)
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 (JWT 사용 시 불필요)
                .csrf(csrf -> csrf.disable())
                
                // 세션 사용 안 함 (JWT는 stateless)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
                // HTTP 요청에 대한 인증/인가 규칙 설정
                .authorizeHttpRequests(auth -> auth
                        // Actuator 엔드포인트
                        .requestMatchers("/actuator/health").permitAll() // 헬스 체크는 공개 (로드밸런서 등에서 사용)
                        .requestMatchers("/actuator/info").permitAll() // 정보는 공개
                        // PERF 프로파일에서는 메트릭 확인을 위해 /actuator/metrics 를 열어둔다.
                        .requestMatchers("/actuator/metrics/**").permitAll()
                        // Prometheus 스크래핑용 (로컬/모니터링; 운영에서는 IP 제한 등 추가 권장)
                        .requestMatchers("/actuator/prometheus").permitAll()
                        .requestMatchers("/actuator/**").authenticated() // 나머지 Actuator 엔드포인트는 인증 필요
                        
                        // Swagger UI 경로 (인증 불필요)
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        
                        // 공개 엔드포인트 (인증 불필요)
                        .requestMatchers("/api/auth/**").permitAll() // 로그인, 회원가입
                        .requestMatchers("/api/users", "/api/users/**").permitAll() // 회원가입은 공개 (선택사항)
                        .requestMatchers("/api/items", "/api/items/**").permitAll() // ✅ 상품 조회는 공개
                        
                        // 관리자 전용 엔드포인트
                        .requestMatchers("/api/admin/**").hasRole("ADMIN") // 관리자만 접근 가능
                        
                        // 나머지 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )
                
                // JWT 인증 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 비밀번호 암호화를 위한 PasswordEncoder 빈 등록
     * 
     * BCryptPasswordEncoder:
     * - BCrypt 해시 알고리즘을 사용한 비밀번호 암호화
     * - 같은 비밀번호라도 매번 다른 해시값 생성 (salt 자동 생성)
     * - 비밀번호 검증 시 matches() 메서드 사용
     * 
     * @return PasswordEncoder 빈
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
