package com.timedeal.api.controller;

import com.timedeal.api.common.ApiPaths;
import com.timedeal.api.dto.item.ItemRequest;
import com.timedeal.api.dto.item.ItemResponse;
import com.timedeal.api.dto.item.ItemSearchCondition;
import com.timedeal.api.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 상품(Item) 관련 REST API Controller
 * 
 * @RestController: 
 * - @Controller + @ResponseBody의 조합
 * - 메서드의 반환값을 HTTP Response Body에 자동으로 JSON/XML로 변환
 * - RESTful API를 만들 때 사용
 * 
 * @RequestMapping("/api/items"):
 * - 클래스 레벨에서 공통 URL 경로 설정
 * - 모든 메서드의 URL 앞에 "/api/items"가 붙음
 * 
 * @RequiredArgsConstructor:
 * - Lombok 어노테이션
 * - final 필드에 대한 생성자를 자동 생성
 * - 의존성 주입(DI)을 위한 생성자 주입 방식 사용
 */
@Tag(name = "상품 API", description = "상품 등록, 조회 등 상품 관련 API")
@RestController
@RequestMapping(ApiPaths.ITEMS)
@RequiredArgsConstructor
public class ItemController {
    
    /**
     * ItemService 의존성 주입
     * 
     * final 키워드: 불변성 보장, 생성자 주입 방식 사용
     * Spring이 자동으로 ItemService 구현체를 주입해줌 (의존성 주입)
     */
    private final ItemService itemService;
    
    /**
     * 상품 등록 API
     * 
     * @PostMapping: HTTP POST 요청을 처리
     * - URL: POST /api/items
     * 
     * @Valid: 
     * - DTO의 유효성 검증 활성화
     * - ItemRequest의 @NotNull, @NotBlank 등의 검증 실행
     * - 검증 실패 시 400 Bad Request 반환
     * 
     * @RequestBody:
     * - HTTP 요청 본문(JSON)을 ItemRequest 객체로 자동 변환
     * - Content-Type: application/json 필요
     * 
     * ResponseEntity:
     * - HTTP 응답을 세밀하게 제어할 수 있는 클래스
     * - 상태 코드, 헤더, 본문을 모두 설정 가능
     * - HttpStatus.CREATED: 201 상태 코드 (리소스 생성 성공)
     */
    @Operation(
            summary = "상품 등록",
            description = "새로운 타임딜 상품을 등록합니다. 상품명, 가격, 오픈 시간, 재고 수량을 입력받습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "상품 등록 성공",
                    content = @Content(schema = @Schema(implementation = ItemResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (필수 필드 누락, 유효성 검증 실패)")
    })
    @PostMapping
    public ResponseEntity<ItemResponse> createItem(@Valid @RequestBody ItemRequest request) {
        ItemResponse response = itemService.createItem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * 상품 조회 API (단건)
     * 
     * @GetMapping("/{id}"): 
     * - HTTP GET 요청 처리
     * - URL: GET /api/items/{id}
     * - {id}는 경로 변수(Path Variable)
     * 
     * @PathVariable:
     * - URL 경로의 {id} 값을 메서드 파라미터로 받음
     * - 예: /api/items/1 → id = 1
     * 
     * ResponseEntity.ok():
     * - 200 OK 상태 코드와 함께 응답 반환
     * - 간단한 성공 응답에 사용
     */
    @Operation(
            summary = "상품 조회",
            description = "상품 ID로 상품 정보를 조회합니다. 재고 수량도 함께 반환됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ItemResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ItemResponse> getItem(
            @Parameter(description = "상품 ID", example = "1", required = true)
            @PathVariable Long id) {
        ItemResponse response = itemService.getItem(id);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 상품 목록 조회 API (페이징 + 검색/필터)
     *
     * 검색 파라미터(모두 선택): name(상품명 포함), minPrice, maxPrice, openAfter, openBefore
     * 페이징: page, size, sort (예: sort=openTime,asc)
     */
    @Operation(
            summary = "상품 목록 조회 (검색/필터 지원)",
            description = "타임딜 상품 목록을 페이징하여 조회합니다. 검색 조건을 주면 조건에 맞는 상품만 반환합니다. " +
                    "검색 파라미터(선택): name(상품명 부분 일치), minPrice(최소 가격), maxPrice(최대 가격), " +
                    "openAfter(오픈 시간 이상), openBefore(오픈 시간 이하). " +
                    "페이징: page, size, sort(예: id,desc 또는 openTime,asc)"
    )
    @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(schema = @Schema(implementation = ItemResponse.class))
    )
    @GetMapping
    public ResponseEntity<Page<ItemResponse>> getItems(
            @ParameterObject ItemSearchCondition condition,
            @ParameterObject
            @PageableDefault(size = 20, sort = "id", direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable) {
        Page<ItemResponse> responses = itemService.getItems(condition, pageable);
        return ResponseEntity.ok(responses);
    }
}
