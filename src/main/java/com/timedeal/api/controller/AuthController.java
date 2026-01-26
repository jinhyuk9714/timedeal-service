package com.timedeal.api.controller;

import com.timedeal.api.dto.auth.LoginRequest;
import com.timedeal.api.dto.auth.LoginResponse;
import com.timedeal.api.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증(Auth) 관련 REST API Controller
 * 
 * @RestController: 
 * - @Controller + @ResponseBody의 조합
 * - 메서드의 반환값을 HTTP Response Body에 자동으로 JSON/XML로 변환
 * - RESTful API를 만들 때 사용
 * 
 * @RequestMapping("/api/auth"):
 * - 클래스 레벨에서 공통 URL 경로 설정
 * - 모든 메서드의 URL 앞에 "/api/auth"가 붙음
 * 
 * @RequiredArgsConstructor:
 * - Lombok 어노테이션
 * - final 필드에 대한 생성자를 자동 생성
 * - 의존성 주입(DI)을 위한 생성자 주입 방식 사용
 * 
 * 인증 관련 엔드포인트:
 * - 로그인: JWT 토큰 발급
 * - (향후 확장 가능: 회원가입, 토큰 갱신 등)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    /**
     * AuthService 의존성 주입
     * 
     * final 키워드: 불변성 보장, 생성자 주입 방식 사용
     * Spring이 자동으로 AuthService 구현체를 주입해줌 (의존성 주입)
     */
    private final AuthService authService;

    /**
     * 사용자 로그인 API
     * 
     * @PostMapping("/login"): HTTP POST 요청을 처리
     * - URL: POST /api/auth/login
     * 
     * @Valid: 
     * - DTO의 유효성 검증 활성화
     * - LoginRequest의 @NotNull, @Email 등의 검증 실행
     * - 검증 실패 시 400 Bad Request 반환
     * 
     * @RequestBody:
     * - HTTP 요청 본문(JSON)을 LoginRequest 객체로 자동 변환
     * - Content-Type: application/json 필요
     * 
     * 인증 프로세스:
     * 1. 이메일로 사용자 조회
     * 2. 비밀번호 검증 (BCrypt)
     * 3. JWT 토큰 생성 및 반환
     * 
     * @param request: 로그인 요청 (이메일, 비밀번호)
     * @return LoginResponse (JWT 토큰, 토큰 타입)
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
