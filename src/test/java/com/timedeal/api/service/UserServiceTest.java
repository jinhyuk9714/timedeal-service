package com.timedeal.api.service;

import com.timedeal.api.domain.user.User;
import com.timedeal.api.domain.user.UserRole;
import com.timedeal.api.dto.user.UserRequest;
import com.timedeal.api.dto.user.UserResponse;
import com.timedeal.api.exception.BusinessException;
import com.timedeal.api.exception.ErrorCode;
import com.timedeal.api.infrastructure.persistence.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UserService 단위 테스트
 * 
 * @ExtendWith(MockitoExtension.class): Mockito를 사용한 단위 테스트
 * @Mock: Mock 객체 생성 (의존성을 대체)
 * @InjectMocks: Mock 객체를 주입받을 대상 객체
 * 
 * 이 테스트는 실제 데이터베이스 없이 Service 로직만 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User user;
    private UserRequest userRequest;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 준비
        user = User.builder()
                .email("test@test.com")
                .password("encodedPassword")
                .name("테스트 사용자")
                .build();
        user.setId(1L);

        userRequest = new UserRequest();
        userRequest.setEmail("test@test.com");
        userRequest.setPassword("password123");
        userRequest.setName("테스트 사용자");
    }

    @Test
    @DisplayName("전체 사용자 목록 조회 성공")
    void getAllUsers_Success() {
        // given: 여러 사용자 데이터
        User user1 = User.builder()
                .email("user1@test.com")
                .password("password1")
                .name("사용자1")
                .build();
        user1.setId(1L);

        User user2 = User.builder()
                .email("user2@test.com")
                .password("password2")
                .name("사용자2")
                .build();
        user2.setId(2L);

        List<User> users = Arrays.asList(user1, user2);

        when(userRepository.findAll()).thenReturn(users);

        // when: 전체 사용자 목록 조회
        List<UserResponse> responses = userService.getAllUsers();

        // then: 검증
        assertThat(responses).isNotNull();
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getEmail()).isEqualTo("user1@test.com");
        assertThat(responses.get(1).getEmail()).isEqualTo("user2@test.com");
        verify(userRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("전체 사용자 목록 조회 - 빈 리스트")
    void getAllUsers_EmptyList() {
        // given: 사용자가 없는 경우
        when(userRepository.findAll()).thenReturn(List.of());

        // when: 전체 사용자 목록 조회
        List<UserResponse> responses = userService.getAllUsers();

        // then: 검증
        assertThat(responses).isNotNull();
        assertThat(responses).isEmpty();
        verify(userRepository, times(1)).findAll();
    }
}
