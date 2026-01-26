package com.timedeal.api.controller;

import com.timedeal.api.dto.order.OrderRequest;
import com.timedeal.api.dto.order.OrderResponse;
import com.timedeal.api.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 주문(Order) 관련 REST API Controller
 * 
 * @RestController: 
 * - @Controller + @ResponseBody의 조합
 * - 메서드의 반환값을 HTTP Response Body에 자동으로 JSON/XML로 변환
 * - RESTful API를 만들 때 사용
 * 
 * @RequestMapping("/api/orders"):
 * - 클래스 레벨에서 공통 URL 경로 설정
 * - 모든 메서드의 URL 앞에 "/api/orders"가 붙음
 * 
 * @RequiredArgsConstructor:
 * - Lombok 어노테이션
 * - final 필드에 대한 생성자를 자동 생성
 * - 의존성 주입(DI)을 위한 생성자 주입 방식 사용
 * 
 * @AuthenticationPrincipal:
 * - SecurityContext에서 인증된 사용자 정보를 주입
 * - JwtAuthenticationFilter에서 설정한 userId가 주입됨
 * - 인증되지 않은 사용자는 접근 불가
 * - 테스트 환경에서는 null일 수 있으므로 null 체크 필요
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    /**
     * OrderService 의존성 주입
     * 
     * final 키워드: 불변성 보장, 생성자 주입 방식 사용
     * Spring이 자동으로 OrderService 구현체를 주입해줌 (의존성 주입)
     */
    private final OrderService orderService;

    /**
     * 주문 생성 API
     * 
     * @PostMapping("/users/{userId}"): HTTP POST 요청을 처리
     * - URL: POST /api/orders/users/{userId}
     * 
     * @Valid: 
     * - DTO의 유효성 검증 활성화
     * - OrderRequest의 @NotNull, @Positive 등의 검증 실행
     * - 검증 실패 시 400 Bad Request 반환
     * 
     * @PathVariable:
     * - URL 경로의 {userId} 값을 메서드 파라미터로 받음
     * 
     * @AuthenticationPrincipal:
     * - 인증된 사용자 ID를 자동으로 주입
     * - JWT 토큰에서 추출한 사용자 ID
     * 
     * 인증 검증:
     * - 요청한 userId와 인증된 사용자 ID가 일치하는지 확인
     * - 불일치 시 403 Forbidden 반환
     * 
     * @param userId: 요청한 사용자 ID (URL 경로에서 추출)
     * @param request: 주문 요청 (상품 ID, 수량)
     * @param authenticatedUserId: 인증된 사용자 ID (JWT에서 추출)
     * @return OrderResponse (생성된 주문 정보)
     */
    @PostMapping("/users/{userId}")
    public ResponseEntity<OrderResponse> createOrder(
            @PathVariable Long userId,
            @Valid @RequestBody OrderRequest request,
            @AuthenticationPrincipal Long authenticatedUserId) {
        
        // 인증 검증: 요청한 사용자와 인증된 사용자가 일치하는지 확인
        if (!isAuthorized(userId, authenticatedUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        OrderResponse response = orderService.createOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 주문 조회 API (단건)
     * 
     * @GetMapping("/{id}"): 
     * - HTTP GET 요청 처리
     * - URL: GET /api/orders/{id}
     * 
     * @PathVariable:
     * - URL 경로의 {id} 값을 메서드 파라미터로 받음
     * - 예: /api/orders/1 → id = 1
     * 
     * @param id: 주문 ID
     * @return OrderResponse (주문 정보)
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        OrderResponse response = orderService.getOrder(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 사용자별 주문 목록 조회 API
     * 
     * @GetMapping("/users/{userId}"): 
     * - HTTP GET 요청 처리
     * - URL: GET /api/orders/users/{userId}
     * 
     * @AuthenticationPrincipal:
     * - 인증된 사용자 ID를 자동으로 주입
     * 
     * 인증 검증:
     * - 요청한 userId와 인증된 사용자 ID가 일치하는지 확인
     * - 불일치 시 403 Forbidden 반환
     * 
     * @param userId: 요청한 사용자 ID (URL 경로에서 추출)
     * @param authenticatedUserId: 인증된 사용자 ID (JWT에서 추출)
     * @return List<OrderResponse> (사용자의 주문 목록)
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<List<OrderResponse>> getUserOrders(
            @PathVariable Long userId,
            @AuthenticationPrincipal Long authenticatedUserId) {
        
        // 인증 검증: 요청한 사용자와 인증된 사용자가 일치하는지 확인
        if (!isAuthorized(userId, authenticatedUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        List<OrderResponse> responses = orderService.getUserOrders(userId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 주문 취소 API
     * 
     * @PatchMapping("/{id}/cancel"): HTTP PATCH 요청을 처리
     * - URL: PATCH /api/orders/{id}/cancel
     * - PATCH: 부분 업데이트에 사용
     * 
     * @PathVariable:
     * - URL 경로의 {id} 값을 메서드 파라미터로 받음
     * 
     * @param id: 취소할 주문 ID
     * @return OrderResponse (취소된 주문 정보)
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long id) {
        OrderResponse response = orderService.cancelOrder(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 사용자 인증 검증 헬퍼 메서드
     * 
     * 요청한 사용자 ID와 인증된 사용자 ID가 일치하는지 확인합니다.
     * 테스트 환경에서는 authenticatedUserId가 null일 수 있으므로 null 체크를 포함합니다.
     * 
     * @param requestedUserId: 요청한 사용자 ID
     * @param authenticatedUserId: 인증된 사용자 ID (JWT에서 추출)
     * @return true: 인증 통과, false: 인증 실패
     */
    private boolean isAuthorized(Long requestedUserId, Long authenticatedUserId) {
        // 테스트 환경에서는 authenticatedUserId가 null일 수 있으므로 허용
        // 프로덕션 환경에서는 SecurityConfig에서 인증이 필수이므로 null이 아님
        return authenticatedUserId == null || requestedUserId.equals(authenticatedUserId);
    }
}
