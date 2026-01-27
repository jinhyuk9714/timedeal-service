package com.timedeal.api.controller;

import com.timedeal.api.common.ApiPaths;
import com.timedeal.api.dto.admin.ChangeRoleRequest;
import com.timedeal.api.dto.item.ItemRequest;
import com.timedeal.api.dto.item.ItemResponse;
import com.timedeal.api.dto.order.OrderResponse;
import com.timedeal.api.dto.user.UserResponse;
import com.timedeal.api.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
@Tag(name = "관리자 API", description = "관리자 전용 API (ADMIN 역할 필요)")
@RestController
@RequestMapping(ApiPaths.ADMIN)
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
     * 전체 주문 목록 조회 (관리자 전용, 페이징)
     * 
     * @GetMapping("/orders"): HTTP GET 요청을 처리
     * - URL: GET /api/admin/orders?page=0&size=10&sort=createdAt,desc
     * 
     * @PreAuthorize("hasRole('ADMIN')"):
     * - 메서드 실행 전 권한 체크
     * - ADMIN 역할을 가진 사용자만 접근 가능
     * - 권한이 없으면 403 Forbidden 반환
     * 
     * @param pageable: 페이징 정보 (page, size, sort)
     * @return Page<OrderResponse> (모든 주문 목록 + 페이징 정보)
     */
    @Operation(
            summary = "전체 주문 목록 조회",
            description = "모든 사용자의 주문 목록을 페이징하여 조회합니다. 관리자(ADMIN) 권한이 필요합니다. " +
                    "쿼리 파라미터: page (페이지 번호, 0부터 시작), size (페이지 크기, 기본값: 20), " +
                    "sort (정렬 기준, 예: createdAt,desc)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (관리자만 접근 가능)")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<OrderResponse>> getAllOrders(
            @ParameterObject
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable) {
        Page<OrderResponse> responses = adminService.getAllOrders(pageable);
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
    @Operation(
            summary = "상품 수정",
            description = "상품 정보를 수정합니다. 관리자(ADMIN) 권한이 필요합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = ItemResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패)"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (관리자만 접근 가능)"),
            @ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/items/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ItemResponse> updateItem(
            @Parameter(description = "상품 ID", example = "1", required = true)
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
    @Operation(
            summary = "상품 삭제",
            description = "상품을 삭제합니다. 관리자(ADMIN) 권한이 필요합니다. " +
                    "주문이 존재하는 상품은 삭제할 수 없습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "400", description = "주문이 존재하는 상품은 삭제할 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (관리자만 접근 가능)"),
            @ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음")
    })
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/items/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteItem(
            @Parameter(description = "상품 ID", example = "1", required = true)
            @PathVariable Long id) {
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
    @Operation(
            summary = "사용자 역할 변경",
            description = "사용자의 역할을 변경합니다. 관리자(ADMIN) 권한이 필요합니다. " +
                    "USER와 ADMIN 역할을 변경할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "역할 변경 성공",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패)"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (관리자만 접근 가능)"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> changeUserRole(
            @Parameter(description = "사용자 ID", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody ChangeRoleRequest request) {
        UserResponse response = adminService.changeUserRole(id, request.getRole());
        return ResponseEntity.ok(response);
    }

    /**
     * 전체 사용자 목록 조회 (관리자 전용, 페이징)
     * 
     * @GetMapping("/users"): HTTP GET 요청을 처리
     * - URL: GET /api/admin/users?page=0&size=10&sort=id,desc
     * 
     * @PreAuthorize("hasRole('ADMIN')"):
     * - 관리자만 접근 가능
     * 
     * @param pageable: 페이징 정보 (page, size, sort)
     * @return Page<UserResponse> (모든 사용자 목록 + 페이징 정보)
     */
    @Operation(
            summary = "전체 사용자 목록 조회",
            description = "모든 사용자 목록을 페이징하여 조회합니다. 관리자(ADMIN) 권한이 필요합니다. " +
                    "쿼리 파라미터: page (페이지 번호, 0부터 시작), size (페이지 크기, 기본값: 20), " +
                    "sort (정렬 기준, 예: id,desc 또는 createdAt,desc)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (관리자만 접근 가능)")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @ParameterObject
            @PageableDefault(size = 20, sort = "id", direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable) {
        Page<UserResponse> responses = adminService.getAllUsers(pageable);
        return ResponseEntity.ok(responses);
    }
}
