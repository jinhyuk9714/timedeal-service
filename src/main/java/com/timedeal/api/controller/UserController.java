package com.timedeal.api.controller;

import com.timedeal.api.dto.user.UserRequest;
import com.timedeal.api.dto.user.UserResponse;
import com.timedeal.api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자(User) 관련 REST API Controller
 * 
 * @RestController: 
 * - @Controller + @ResponseBody의 조합
 * - 메서드의 반환값을 HTTP Response Body에 자동으로 JSON/XML로 변환
 * - RESTful API를 만들 때 사용
 * 
 * @RequestMapping("/api/users"):
 * - 클래스 레벨에서 공통 URL 경로 설정
 * - 모든 메서드의 URL 앞에 "/api/users"가 붙음
 * 
 * @RequiredArgsConstructor:
 * - Lombok 어노테이션
 * - final 필드에 대한 생성자를 자동 생성
 * - 의존성 주입(DI)을 위한 생성자 주입 방식 사용
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    /**
     * UserService 의존성 주입
     * 
     * final 키워드: 불변성 보장, 생성자 주입 방식 사용
     * Spring이 자동으로 UserService 구현체를 주입해줌 (의존성 주입)
     */
    private final UserService userService;
    
    /**
     * 사용자 회원가입 API
     * 
     * @PostMapping: HTTP POST 요청을 처리
     * - URL: POST /api/users
     * 
     * @Valid: 
     * - DTO의 유효성 검증 활성화
     * - UserRequest의 @NotNull, @NotBlank, @Email 등의 검증 실행
     * - 검증 실패 시 400 Bad Request 반환
     * 
     * @RequestBody:
     * - HTTP 요청 본문(JSON)을 UserRequest 객체로 자동 변환
     * - Content-Type: application/json 필요
     * 
     * ResponseEntity:
     * - HTTP 응답을 세밀하게 제어할 수 있는 클래스
     * - HttpStatus.CREATED: 201 상태 코드 (리소스 생성 성공)
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * 사용자 조회 API (단건)
     * 
     * @GetMapping("/{id}"): 
     * - HTTP GET 요청 처리
     * - URL: GET /api/users/{id}
     * - {id}는 경로 변수(Path Variable)
     * 
     * @PathVariable:
     * - URL 경로의 {id} 값을 메서드 파라미터로 받음
     * - 예: /api/users/1 → id = 1
     * 
     * ResponseEntity.ok():
     * - 200 OK 상태 코드와 함께 응답 반환
     * - 간단한 성공 응답에 사용
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        UserResponse response = userService.getUser(id);
        return ResponseEntity.ok(response);
    }
}
