package com.timedeal.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.timedeal.api.common.ApiPaths;
import com.timedeal.api.dto.auth.LoginRequest;
import com.timedeal.api.dto.auth.LoginResponse;
import com.timedeal.api.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 통합 테스트
 * 
 * @WebMvcTest: 웹 레이어만 테스트하는 어노테이션
 * - Controller, Filter, Interceptor 등 웹 관련 빈만 로드
 * - Service는 Mock으로 대체 (@MockitoBean 사용)
 * - 빠른 테스트 실행 가능
 * 
 * @ActiveProfiles("test"):
 * - test 프로파일 활성화하여 TestSecurityConfig 사용
 * 
 * @AutoConfigureMockMvc(addFilters = false):
 * - Security 필터를 제외하여 인증 없이 API를 테스트
 */
@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc; // HTTP 요청/응답을 시뮬레이션하는 객체

    private ObjectMapper objectMapper; // JSON 변환을 위한 객체

    @MockitoBean
    private AuthService authService; // Service는 Mock으로 대체

    @MockitoBean
    private com.timedeal.api.infrastructure.security.JwtTokenProvider jwtTokenProvider; // Security 관련 빈 Mock 처리

    @MockitoBean
    private com.timedeal.api.infrastructure.security.TokenBlacklistService tokenBlacklistService; // Redis 관련 빈 Mock 처리

    @MockitoBean
    private com.timedeal.api.infrastructure.security.JwtAuthenticationFilter jwtAuthenticationFilter; // JWT 필터 Mock 처리

    @BeforeEach
    void setUp() {
        // ObjectMapper 직접 생성
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("로그인 성공")
    void login_Success() throws Exception {
        // given: 테스트 데이터
        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("password123");

        LoginResponse response = new LoginResponse("jwt-token", "USER");

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        // when & then: HTTP 요청 실행 및 검증
        mockMvc.perform(post(ApiPaths.AUTH + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("로그인 실패 - 유효성 검증 실패")
    void login_ValidationFailed() throws Exception {
        // given: 잘못된 데이터 (이메일이 null)
        LoginRequest request = new LoginRequest();
        request.setPassword("password123");
        // email이 null이므로 유효성 검증 실패 예상

        // when & then: HTTP 요청 실행 및 검증
        mockMvc.perform(post(ApiPaths.AUTH + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // 400 상태 코드
    }

    @Test
    @DisplayName("로그아웃 성공")
    void logout_Success() throws Exception {
        // given: Authorization 헤더에 토큰 포함
        String token = "jwt-token";

        // when & then: HTTP 요청 실행 및 검증
        mockMvc.perform(post(ApiPaths.AUTH + "/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("로그아웃 실패 - Authorization 헤더 없음")
    void logout_NoAuthorizationHeader_Fail() throws Exception {
        // when & then: Authorization 헤더가 없으므로 400 반환
        mockMvc.perform(post(ApiPaths.AUTH + "/logout"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그아웃 실패 - Bearer 접두사 없음")
    void logout_NoBearerPrefix_Fail() throws Exception {
        // when & then: Bearer 접두사가 없으므로 400 반환
        mockMvc.perform(post(ApiPaths.AUTH + "/logout")
                        .header("Authorization", "jwt-token"))
                .andExpect(status().isBadRequest());
    }
}
