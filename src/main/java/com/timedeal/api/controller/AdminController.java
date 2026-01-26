package com.timedeal.api.controller;

import com.timedeal.api.dto.admin.ChangeRoleRequest;
import com.timedeal.api.dto.item.ItemRequest;
import com.timedeal.api.dto.item.ItemResponse;
import com.timedeal.api.dto.order.OrderResponse;
import com.timedeal.api.dto.user.UserResponse;
import com.timedeal.api.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 관리자 전용 REST API Controller
 * 
 * @RestController: 
 * - @Controller + @ResponseBody의 조합
 * - 메서드의 반환값을 HTTP Response Body에 자동으로 JSON/XML로 변환
 * - RESTful API를 만들 때 사용
 * 
 * @RequestMapping("/api/admin"):
 * - 클래스 레벨에서 공통 URL 경로 설정
 * - 모든 메서드의 URL 앞에 "/api/admin"이 붙음
 * 
 * @RequiredArgsConstructor:
 * - Lombok 어노테이션
 * - final 필드에 대한 생성자를 자동 생성
 * - 의존성 주입(DI)을 위한 생성자 주입 방식 사용
 * 
 * @PreAuthorize("hasRole('ADMIN')"):
 * - 메서드 실행 전 권한 체크
 * - ADMIN 역할을 가진 사용자만 접근 가능
 * - SecurityConfig에서 /api/admin/** 경로에 hasRole("ADMIN") 설정이 있어도
 *   메서드 레벨에서 명시적으로 체크하는 것이 더 안전함
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    /**
     * AdminService 의존성 주입
     * 
     * final 키워드: 불변성 보장, 생성자 주입 방식 사용
     * Spring이 자동으로 AdminService 구현체를 주입해줌 (의존성 주입)
     */
    private final AdminService adminService;

    /**
     * 전체 주문 목록 조회 (관리자 전용)
     * 
     * @GetMapping("/orders"): HTTP GET 요청을 처리
     * - URL: GET /api/admin/orders
     * 
     * @PreAuthorize("hasRole('ADMIN')"):
     * - 메서드 실행 전 권한 체크
     * - ADMIN 역할을 가진 사용자만 접근 가능
     * - 권한이 없으면 403 Forbidden 반환
     * 
     * @return List<OrderResponse> (모든 주문 목록)
     */
    @GetMapping("/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        List<OrderResponse> responses = adminService.getAllOrders();
        return ResponseEntity.ok(responses);
    }

    /**
     * 상품 수정 (관리자 전용)
     * 
     * @PutMapping("/items/{id}"): HTTP PUT 요청을 처리
     * - URL: PUT /api/admin/items/{id}
     * - PUT: 전체 리소스 업데이트에 사용
     * 
     * @PathVariable:
     * - URL 경로의 {id} 값을 메서드 파라미터로 받음
     * 
     * @Valid: 
     * - DTO의 유효성 검증 활성화
     * - ItemRequest의 @NotNull, @NotBlank 등의 검증 실행
     * - 검증 실패 시 400 Bad Request 반환
     * 
     * @PreAuthorize("hasRole('ADMIN')"):
     * - 관리자만 접근 가능
     * 
     * @param id: 수정할 상품 ID
     * @param request: 수정할 상품 정보
     * @return ItemResponse (수정된 상품 정보)
     */
    @PutMapping("/items/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ItemResponse> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody ItemRequest request) {
        ItemResponse response = adminService.updateItem(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 상품 삭제 (관리자 전용)
     * 
     * @DeleteMapping("/items/{id}"): HTTP DELETE 요청을 처리
     * - URL: DELETE /api/admin/items/{id}
     * 
     * @PathVariable:
     * - URL 경로의 {id} 값을 메서드 파라미터로 받음
     * 
     * @PreAuthorize("hasRole('ADMIN')"):
     * - 관리자만 접근 가능
     * 
     * @param id: 삭제할 상품 ID
     * @return 204 No Content (성공 응답)
     */
    @DeleteMapping("/items/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        adminService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 사용자 역할 변경 (관리자 전용)
     * 
     * @PatchMapping("/users/{id}/role"): HTTP PATCH 요청을 처리
     * - URL: PATCH /api/admin/users/{id}/role
     * - PATCH: 부분 업데이트에 사용
     * 
     * @PathVariable:
     * - URL 경로의 {id} 값을 메서드 파라미터로 받음
     * 
     * @Valid: 
     * - DTO의 유효성 검증 활성화
     * 
     * @PreAuthorize("hasRole('ADMIN')"):
     * - 관리자만 접근 가능
     * 
     * @param id: 역할을 변경할 사용자 ID
     * @param request: 변경할 역할 (USER, ADMIN)
     * @return UserResponse (변경된 사용자 정보)
     */
    @PatchMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> changeUserRole(
            @PathVariable Long id,
            @Valid @RequestBody ChangeRoleRequest request) {
        UserResponse response = adminService.changeUserRole(id, request.getRole());
        return ResponseEntity.ok(response);
    }

    /**
     * 전체 사용자 목록 조회 (관리자 전용)
     * 
     * @GetMapping("/users"): HTTP GET 요청을 처리
     * - URL: GET /api/admin/users
     * 
     * @PreAuthorize("hasRole('ADMIN')"):
     * - 관리자만 접근 가능
     * 
     * @return List<UserResponse> (모든 사용자 목록)
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> responses = adminService.getAllUsers();
        return ResponseEntity.ok(responses);
    }
}
