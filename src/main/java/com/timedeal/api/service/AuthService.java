package com.timedeal.api.service;

import com.timedeal.api.domain.user.User;
import com.timedeal.api.dto.auth.LoginRequest;
import com.timedeal.api.dto.auth.LoginResponse;
import com.timedeal.api.exception.BusinessException;
import com.timedeal.api.exception.ErrorCode;
import com.timedeal.api.infrastructure.persistence.user.UserRepository;
import com.timedeal.api.infrastructure.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 관련 비즈니스 로직을 처리하는 Service 클래스
 * 
 * 로그인, 토큰 발급 등의 인증 작업을 담당합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 사용자 로그인 처리
     * 
     * @param request: 로그인 요청 (이메일, 비밀번호)
     * @return LoginResponse (JWT 토큰)
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        // 1. 이메일로 사용자 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        // 2. 비밀번호 검증
        // - passwordEncoder.matches(): 평문 비밀번호와 암호화된 비밀번호 비교
        // - 일치하지 않으면 예외 발생
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 3. JWT 토큰 생성
        String token = jwtTokenProvider.generateToken(user.getId());

        // 4. 응답 반환
        return new LoginResponse(token);
    }
}
