package com.timedeal.api.service;

import com.timedeal.api.domain.user.User;
import com.timedeal.api.dto.auth.LoginRequest;
import com.timedeal.api.dto.auth.LoginResponse;
import com.timedeal.api.exception.BusinessException;
import com.timedeal.api.exception.ErrorCode;
import com.timedeal.api.infrastructure.persistence.user.UserRepository;
import com.timedeal.api.infrastructure.security.JwtTokenProvider;
import com.timedeal.api.infrastructure.security.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 관련 비즈니스 로직을 처리하는 Service 클래스
 * 
 * 로그인, 로그아웃, 토큰 발급 등의 인증 작업을 담당합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;

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

        // 3. JWT 토큰 생성 (사용자 ID와 Role 포함)
        String role = user.getRole() != null ? user.getRole().name() : null;
        String token = jwtTokenProvider.generateToken(user.getId(), role);

        // 4. 응답 반환 (role 포함하여 클라이언트에서 구분 가능)
        return new LoginResponse(token, role);
    }

    /**
     * 사용자 로그아웃 처리
     * 
     * JWT는 stateless이므로 서버에서 토큰을 무효화할 수 없습니다.
     * 따라서 로그아웃된 토큰을 Redis 블랙리스트에 추가하여
     * 이후 요청에서 해당 토큰을 거부합니다.
     * 
     * @param token: 로그아웃할 JWT 토큰
     */
    @Transactional
    public void logout(String token) {
        // 1. 토큰 유효성 검증
        if (!jwtTokenProvider.validateToken(token)) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 2. 토큰의 만료 시간 추출
        long expirationTime = jwtTokenProvider.getExpirationTime(token);

        // 3. 블랙리스트에 추가
        // - 토큰 만료 시간까지 Redis에 보관
        // - 이후 요청에서 이 토큰은 거부됨
        tokenBlacklistService.addToBlacklist(token, expirationTime);
    }
}
