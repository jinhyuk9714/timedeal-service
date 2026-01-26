package com.timedeal.api.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 토큰을 검증하고 인증 정보를 설정하는 필터
 * 
 * @Component:
 * - Spring이 자동으로 빈으로 등록
 * 
 * OncePerRequestFilter:
 * - 요청당 한 번만 실행되는 필터
 * - Spring Security 필터 체인에 추가됨
 * 
 * 필터(Filter):
 * - HTTP 요청이 Controller에 도달하기 전에 실행되는 컴포넌트
 * - 인증, 로깅, 인코딩 등의 공통 작업 처리
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * HTTP 요청에서 JWT 토큰을 추출하고 검증하여 인증 정보를 설정
     * 
     * @param request: HTTP 요청
     * @param response: HTTP 응답
     * @param filterChain: 다음 필터로 요청을 전달하는 체인
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // 1. 요청 헤더에서 JWT 토큰 추출
        String token = getTokenFromRequest(request);

        // 2. 토큰이 있고 유효한 경우
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            // 3. 토큰에서 사용자 ID 추출
            Long userId = jwtTokenProvider.getUserIdFromToken(token);

            // 4. 인증 객체 생성
            // - UsernamePasswordAuthenticationToken: Spring Security의 인증 객체
            // - principal: 인증된 사용자 정보 (여기서는 userId)
            // - authorities: 사용자의 권한 (현재는 빈 리스트)
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId, // principal (인증된 사용자)
                            null, // credentials (비밀번호, JWT에서는 불필요)
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")) // 권한
                    );

            // 5. 인증 객체에 요청 정보 추가
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // 6. SecurityContext에 인증 정보 저장
            // - SecurityContext: 현재 요청의 보안 컨텍스트
            // - Controller에서 @AuthenticationPrincipal로 접근 가능
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 7. 다음 필터로 요청 전달
        filterChain.doFilter(request, response);
    }

    /**
     * HTTP 요청 헤더에서 JWT 토큰을 추출
     * 
     * @param request: HTTP 요청
     * @return JWT 토큰 문자열 (없으면 null)
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        // "Bearer " 접두사 제거
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        return null;
    }
}
