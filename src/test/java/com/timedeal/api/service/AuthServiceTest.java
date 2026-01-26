package com.timedeal.api.service;

import com.timedeal.api.domain.user.User;
import com.timedeal.api.domain.user.UserRole;
import com.timedeal.api.dto.auth.LoginRequest;
import com.timedeal.api.dto.auth.LoginResponse;
import com.timedeal.api.exception.BusinessException;
import com.timedeal.api.exception.ErrorCode;
import com.timedeal.api.infrastructure.persistence.user.UserRepository;
import com.timedeal.api.infrastructure.security.JwtTokenProvider;
import com.timedeal.api.infrastructure.security.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * AuthService 단위 테스트
 * 
 * @ExtendWith(MockitoExtension.class): Mockito를 사용한 단위 테스트
 * @Mock: Mock 객체 생성 (의존성을 대체)
 * @InjectMocks: Mock 객체를 주입받을 대상 객체
 * 
 * 이 테스트는 실제 데이터베이스 없이 Service 로직만 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private AuthService authService;

    private User user;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 준비
        user = User.builder()
                .email("test@test.com")
                .password("encodedPassword")
                .name("테스트 사용자")
                .build();
        user.setId(1L);

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@test.com");
        loginRequest.setPassword("password123");
    }

    @Test
    @DisplayName("로그인 성공 - 일반 사용자")
    void login_Success_User() {
        // given: Mock 객체의 동작 정의
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtTokenProvider.generateToken(1L, "USER")).thenReturn("jwt-token");

        // when: 로그인
        LoginResponse response = authService.login(loginRequest);

        // then: 검증
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getRole()).isEqualTo("USER");
        verify(userRepository, times(1)).findByEmail("test@test.com");
        verify(passwordEncoder, times(1)).matches("password123", "encodedPassword");
        verify(jwtTokenProvider, times(1)).generateToken(1L, "USER");
    }

    @Test
    @DisplayName("로그인 성공 - 관리자")
    void login_Success_Admin() {
        // given: 관리자 사용자
        User adminUser = User.builder()
                .email("admin@test.com")
                .password("encodedPassword")
                .name("관리자")
                .build();
        adminUser.setId(2L);
        adminUser.changeRole(UserRole.ADMIN);

        LoginRequest adminLoginRequest = new LoginRequest();
        adminLoginRequest.setEmail("admin@test.com");
        adminLoginRequest.setPassword("password123");

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtTokenProvider.generateToken(2L, "ADMIN")).thenReturn("admin-jwt-token");

        // when: 로그인
        LoginResponse response = authService.login(adminLoginRequest);

        // then: 검증
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("admin-jwt-token");
        assertThat(response.getRole()).isEqualTo("ADMIN");
        verify(jwtTokenProvider, times(1)).generateToken(2L, "ADMIN");
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일")
    void login_UserNotFound_Fail() {
        // given: 존재하지 않는 이메일
        when(userRepository.findByEmail("notfound@test.com")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setEmail("notfound@test.com");
        request.setPassword("password123");

        // when & then: 사용자를 찾을 수 없으므로 예외 발생
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.INVALID_CREDENTIALS.getMessage());
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    void login_InvalidPassword_Fail() {
        // given: 비밀번호가 일치하지 않음
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("wrongPassword");

        // when & then: 비밀번호가 일치하지 않으므로 예외 발생
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.INVALID_CREDENTIALS.getMessage());
    }

    @Test
    @DisplayName("로그아웃 성공")
    void logout_Success() {
        // given: 유효한 토큰
        String token = "valid-jwt-token";
        long expirationTime = System.currentTimeMillis() + 86400000; // 24시간 후

        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getExpirationTime(token)).thenReturn(expirationTime);
        doNothing().when(tokenBlacklistService).addToBlacklist(anyString(), anyLong());

        // when: 로그아웃
        authService.logout(token);

        // then: 검증
        verify(jwtTokenProvider, times(1)).validateToken(token);
        verify(jwtTokenProvider, times(1)).getExpirationTime(token);
        verify(tokenBlacklistService, times(1)).addToBlacklist(token, expirationTime);
    }

    @Test
    @DisplayName("로그아웃 실패 - 유효하지 않은 토큰")
    void logout_InvalidToken_Fail() {
        // given: 유효하지 않은 토큰
        String invalidToken = "invalid-jwt-token";

        when(jwtTokenProvider.validateToken(invalidToken)).thenReturn(false);

        // when & then: 유효하지 않은 토큰이므로 예외 발생
        assertThatThrownBy(() -> authService.logout(invalidToken))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.INVALID_CREDENTIALS.getMessage());

        verify(jwtTokenProvider, times(1)).validateToken(invalidToken);
        verify(tokenBlacklistService, never()).addToBlacklist(anyString(), anyLong());
    }
}
