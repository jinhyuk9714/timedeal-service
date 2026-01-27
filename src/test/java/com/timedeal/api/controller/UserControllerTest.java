package com.timedeal.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.timedeal.api.domain.user.User;
import com.timedeal.api.common.ApiPaths;
import com.timedeal.api.dto.user.UserRequest;
import com.timedeal.api.dto.user.UserResponse;
import com.timedeal.api.service.UserService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserController 통합 테스트
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
@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc; // HTTP 요청/응답을 시뮬레이션하는 객체

    private ObjectMapper objectMapper; // JSON 변환을 위한 객체

    @MockitoBean
    private UserService userService; // Service는 Mock으로 대체

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
    @DisplayName("회원가입 성공")
    void createUser_Success() throws Exception {
        // given: 테스트 데이터 - 실제 User 객체 생성
        User user = User.builder()
                .email("test@test.com")
                .password("encodedPassword")
                .name("테스트 사용자")
                .build();
        user.setId(1L);

        UserRequest request = new UserRequest();
        request.setEmail("test@test.com");
        request.setPassword("password123");
        request.setName("테스트 사용자");

        UserResponse response = new UserResponse(user);

        when(userService.createUser(any(UserRequest.class))).thenReturn(response);

        // when & then: HTTP 요청 실행 및 검증
        mockMvc.perform(post(ApiPaths.USERS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()) // 201 상태 코드
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("test@test.com"))
                .andExpect(jsonPath("$.name").value("테스트 사용자"));
    }

    @Test
    @DisplayName("회원가입 실패 - 유효성 검증 실패")
    void createUser_ValidationFailed() throws Exception {
        // given: 잘못된 데이터 (이메일이 null)
        UserRequest request = new UserRequest();
        request.setPassword("password123");
        request.setName("테스트 사용자");
        // email이 null이므로 유효성 검증 실패 예상

        // when & then: HTTP 요청 실행 및 검증
        // @WebMvcTest에서는 유효성 검증이 제대로 작동하지 않을 수 있으므로
        // 실제로는 Service가 호출되지 않을 수 있습니다.
        // 하지만 유효성 검증 실패 시 400을 반환하는 것은 Controller의 기본 동작입니다.
        mockMvc.perform(post(ApiPaths.USERS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // 400 상태 코드 (유효성 검증 실패)
    }

    @Test
    @DisplayName("사용자 조회 성공")
    void getUser_Success() throws Exception {
        // given: Mock 데이터 - 실제 User 객체 생성
        User user = User.builder()
                .email("test@test.com")
                .password("password")
                .name("테스트 사용자")
                .build();
        user.setId(1L);

        UserResponse response = new UserResponse(user);

        when(userService.getUser(eq(1L))).thenReturn(response);

        // when & then: HTTP 요청 실행 및 검증
        mockMvc.perform(get(ApiPaths.USERS + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("test@test.com"))
                .andExpect(jsonPath("$.name").value("테스트 사용자"));
    }
}
